import os
import time
import torch
import random
import numpy as np
from torch.utils.tensorboard import SummaryWriter

from log.logger import Logger, setup_logger
from validate import validate
from data import create_dataloader
from earlystop import EarlyStopping
from networks.trainer import Trainer
from options.train_options import TrainOptions


"""Currently assumes jpg_prob, blur_prob 0 or 1"""
def get_val_opt():
    val_opt = TrainOptions().parse(print_options=False)
    val_opt.isTrain = False
    val_opt.no_resize = False
    val_opt.no_crop = True
    val_opt.no_flip = True
    val_opt.serial_batches = True
    val_opt.data_label = 'val' # 用于辨识
    # val_opt.jpg_method = ['pil']
    # val_opt.blur_prob = 0.0
    # val_opt.jpg_prob = 0.0
    val_opt.data_aug = False
    val_opt.GaussianNoise = False
    val_opt.randomErasing = False
    # if len(val_opt.blur_sig) == 2:  # set blur_augment
    #     b_sig = val_opt.blur_sig
    #     val_opt.blur_sig = [(b_sig[0] + b_sig[1]) / 2]
    # if len(val_opt.jpg_qual) != 1:
    #     j_qual = val_opt.jpg_qual
    #     val_opt.jpg_qual = [int((j_qual[0] + j_qual[-1]) / 2)]

    return val_opt

SEED = 0
def set_seed():
    torch.manual_seed(SEED)
    torch.cuda.manual_seed(SEED)
    np.random.seed(SEED)
    random.seed(SEED)

if __name__ == '__main__':
    log_dir = "./log"
    setup_logger(log_dir)

    set_seed()
    print("Set Seed:", SEED)

    print("Training options:")
    opt = TrainOptions().parse()
    val_opt = get_val_opt()
    print("-----------------------------------------")
    print("Validation options:")
    for k, v in sorted(vars(val_opt).items()):
        print(f"{k}: {v}")
    print("----------------- End -------------------")

    model = Trainer(opt)
    # model.load_ckpt("./pretrained_weights/10_21_20_52.pth")

    print("-----------------------------------------")
    print("Train Dataset:")
    data_loader = create_dataloader(opt)
    print("-----------------------------------------")
    print("Valid Dataset:")
    val_loader = create_dataloader(val_opt)
    print("----------------- End -------------------")

    train_writer = SummaryWriter(os.path.join(opt.checkpoints_dir, opt.name, "train"))
    val_writer = SummaryWriter(os.path.join(opt.checkpoints_dir, opt.name, "val"))
        
    early_stopping = EarlyStopping(patience=opt.earlystop_epoch, delta=-0.001, verbose=True)
    start_time = time.time()
    print ("Length of data loader: %d" %(len(data_loader)))
    for epoch in range(opt.niter):
        
        for i, data in enumerate(data_loader):
            model.total_steps += 1

            model.set_input(data)
            model.optimize_parameters()

            if model.total_steps % opt.loss_freq == 0:
                print("Train loss: {} at step: {}".format(model.loss, model.total_steps))
                train_writer.add_scalar('loss', model.loss, model.total_steps)
                print("Iter time: ", ((time.time()-start_time)/model.total_steps)  )

            if model.total_steps in [10,30,50,100,1000,5000,10000] and False: # save models at these iters 
                model.save_networks('model_iters_%s.pth' % model.total_steps)

        if epoch % opt.save_epoch_freq == 0:
            print('saving the model at the end of epoch %d' % (epoch))
            model.save_networks( 'model_epoch_best.pth' )
            model.save_networks( 'model_epoch_%s.pth' % epoch )

        # Validation
        model.eval()
        ap, r_acc, f_acc, acc = validate(model.model, val_loader)
        val_writer.add_scalar('accuracy', acc, model.total_steps)
        val_writer.add_scalar('ap', ap, model.total_steps)
        print("\n(Val @ epoch {}) acc: {}; ap: {}".format(epoch, acc, ap))

        early_stopping(acc, model)
        if early_stopping.early_stop:
            cont_train = model.adjust_learning_rate()
            if cont_train:
                print("Learning rate dropped by 10, continue training...")
                early_stopping = EarlyStopping(patience=opt.earlystop_epoch, delta=-0.002, verbose=True)
            else:
                print("Early stopping.")
                break
        model.train()
        
    end_time = time.time()
    total_time = end_time - start_time
    minutes = int(total_time // 60)
    seconds = int(total_time % 60)
    print(f"Training completed in {minutes} minutes and {seconds} seconds.")
