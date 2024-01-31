from django.http import JsonResponse
from io import BytesIO
from PIL import Image
from . import utils
import cv2
import numpy as np
import requests

# Create your views here.
def plate_recog(request):
    if request.method == 'POST':
        img_out = Image.open(BytesIO(request.body))
        img_out = np.array(img_out)
        img_out = cv2.cvtColor(img_out, cv2.COLOR_BGR2RGB)
        
        cv2.imwrite('./image.png', img_out)
        text = utils.main('./image.png') # 여기에 번호판 인식 함수랑 집어 넣고 돌려서 보내
        return JsonResponse({'text':text})
    else:
        return JsonResponse({'error':'error'})


def entrance(request):
    if request.method == 'POST':
        car_data = BytesIO(request.body)
        entrance_url = 'http://localhost:8080/park/allocation/'
        response = requests.post(entrance_url, data=car_data)
        result = response.json()
        return JsonResponse()


def hall(request):
    if request.method == 'POST':
        car_data = BytesIO(request.body)
        park_url = 'http://localhost:8080/park/validation/'
        response = requests.post(park_url, data=car_data)
        result = response.json()
        park_id = result['park_id'][0]
        return JsonResponse({'park_id':park_id})


def exit_way(request):
    if request.method == 'POST':
        car_data = BytesIO(request.body)
        exit_url = 'http://localhost:8080/park/out/'
        response = requests.post(exit_url, data=car_data)
        result = response.json()
        return JsonResponse()


def bar(request):
    if request.method == 'POST':
        bar_url = 'http://localhost:8080/admin/bar/'
        response = requests.post(bar_url)
        result = response.json()
        park_no = result['park_no'][0]
        return JsonResponse({'park_no':park_no})