CUDA_VISIBLE_DEVICES=4                               \
python train.py --name=clip_vitl14                      \
--real_list_path=/home/data/szk/test_dataset_11_8/0_real                 \
--fake_list_path=/home/data/szk/test_dataset_11_8/1_fake          \
--gpu_ids=0                                               \
--batch_size=16                                          \
--suffix=time                                            \
--data_mode=ours  --arch=CLIP:ViT-L/14  --fix_backbone  \
# --focalloss
# --GaussianNoise                                         \
# --RandomErasing                                         \
                              
# 注意选定的GPU是否空闲！！！