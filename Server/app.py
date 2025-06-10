import os
import base64
import numpy as np
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse,FileResponse
from fastapi.middleware.cors import CORSMiddleware
from inference import model_prepare, predict

# 初始化 FastAPI 应用程序
app = FastAPI()
app.config = {'UPLOAD_FOLDER': './uploads'}
detect_model = model_prepare()
# 添加 CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# 创建上传文件夹
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)


@app.post('/detect')
async def api_detect_fake_or_true(file: UploadFile = File(...)):
    """文件上传接口"""
    if not file.filename:
        raise HTTPException(status_code=400, detail="空文件名")
    if not file:
        raise HTTPException(status_code=400, detail="未上传文件")

    try:
        filename = file.filename
        # 保存文件
        save_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        with open(save_path, 'wb') as f:
            f.write(file.file.read())
        confidence = predict(save_path, detect_model)
        # 确保 confidence 是可序列化的类型
        if isinstance(confidence, np.ndarray):
            confidence = confidence.item()  # 将 NumPy 数组转换为 Python 原生类型
        return JSONResponse(content={"message": "文件检测成功", "confidence": confidence}, status_code=200)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if os.path.exists(save_path):
            os.remove(save_path)  # 清理临时文件

@app.get('/get_image')
async def api_get_image():
    try:
        image_path = "boxed_faces/face_with_box.jpg"
        # 检查图片是否存在
        if os.path.exists(image_path):
            # 返回图片base64作为响应
            image_base64 = upload_image_to_base64(image_path)
            return JSONResponse(content={"message": "图片返回成功", "image": image_base64}, status_code=200)
        else:
            return {"error": "Image not found"}, 404
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def upload_image_to_base64(image_path):
    with open(image_path, "rb") as image_file:
        encoded_string = base64.b64encode(image_file.read()).decode('utf-8')
    return encoded_string

# 启动 FastAPI 应用程序
if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=50005)