import json
import os
import urllib.request
from collections import deque
import cv2
import numpy as np
from flask import Flask, flash, request, redirect, url_for, render_template
from werkzeug.utils import secure_filename
import tensorflow as tf
# Server based on: https://roytuts.com/upload-and-play-video-using-flask/


app = Flask(__name__)
UPLOAD_FOLDER = './static/uploads'
app.secret_key = "secret key"
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
basedir = os.path.abspath(os.path.dirname(__file__))
classes_list = ["clap", "wave"]
image_height, image_width = 64, 64

window_size = 25
prediction_folder = './static/predictions'
model_path = os.path.join(basedir, './Classifier', 'Model___Date_Time_2022_04_06__18_21_54___Loss_0.6936672329902649___Accuracy_0.25.h5')
model = tf.keras.models.load_model(model_path)
@app.route('/')
def upload_form():
    return render_template('upload.html')


@app.route('/', methods=['POST'])
def upload_video():
    if 'file' not in request.files:
        flash('No file part')
        return redirect(request.url)
    file = request.files['file']
    if file.filename == '':
        flash('No image selected for uploading')
        return redirect(request.url)
    else:
        filename = secure_filename(file.filename)
        # Fix file not found: https: // stackoverflow.com / questions / 37901716 / flask - uploads - ioerror - errno - 2 - no - such - file - or -directory
        file.save(os.path.join(basedir, app.config['UPLOAD_FOLDER'], filename))
        # print('upload_video filename: ' + filename)
        flash('Video successfully uploaded and displayed below')
        output_video_file_path = f'{prediction_folder}/{filename}.mp4'

        prediction, probability = predict_on_live_video(os.path.join(basedir, app.config['UPLOAD_FOLDER'], filename), output_video_file_path, 25, model)
        print(probability)
        prediction_dict = {'prediction': prediction, 'probability': probability}
        response = app.response_class(
            response= json.dumps(prediction_dict),
            status=200,
            mimetype='application/json'
        )
        return response


@app.route('/display/<filename>')
def display_video(filename):
    # print('display_video filename: ' + filename)
    return redirect(url_for('static', filename='uploads/' + filename), code=301)


# From juypiter notbook couldnt import, modified for server call
def predict_on_live_video(video_file_path, output_file_path, window_size, model):
    # Initialize a Deque Object with a fixed size which will be used to implement moving/rolling average functionality.
    predicted_labels_probabilities_deque = deque(maxlen=window_size)

    # Reading the Video File using the VideoCapture Object
    video_reader = cv2.VideoCapture(video_file_path)
    predicted_class_name = ''
    probability = ''

    while True:

        # Reading The Frame
        status, frame = video_reader.read()

        if not status:
            break

        # Resize the Frame to fixed Dimensions
        resized_frame = cv2.resize(frame, (image_height, image_width))

        # Normalize the resized frame by dividing it with 255 so that each pixel value then lies between 0 and 1
        normalized_frame = resized_frame / 255

        # Passing the Image Normalized Frame to the model and receiving Predicted Probabilities.
        predicted_labels_probabilities = model.predict(np.expand_dims(normalized_frame, axis=0))[0]

        # Appending predicted label probabilities to the deque object
        predicted_labels_probabilities_deque.append(predicted_labels_probabilities)

        # Assuring that the Deque is completely filled before starting the averaging process
        if len(predicted_labels_probabilities_deque) == window_size:
            # Converting Predicted Labels Probabilities Deque into Numpy array
            predicted_labels_probabilities_np = np.array(predicted_labels_probabilities_deque)

            # Calculating Average of Predicted Labels Probabilities Column Wise
            predicted_labels_probabilities_averaged = predicted_labels_probabilities_np.mean(axis=0)

            # Converting the predicted probabilities into labels by returning the index of the maximum value.
            predicted_label = np.argmax(predicted_labels_probabilities_averaged)

            # get the probability for the predicted label
            probability = predicted_labels_probabilities_averaged[predicted_label]
            # Accessing The Class Name using predicted label.
            predicted_class_name = classes_list[predicted_label]

    # Closing the VideoCapture object and releasing all resources held by them.
    video_reader.release()
    return predicted_class_name, probability


if __name__ == '__main__':
    app.run()
