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
classes_list = ["clap", "wave", "boogie", 'kisses', 'mind_blown']
model_output_size = len(classes_list)
image_height, image_width = 64, 64

window_size = 10
prediction_folder = './static/predictions'
model_path = os.path.join(basedir, './Classifier', 'Model___Date_Time_2022_04_09__02_08_12___Loss_1.6582568883895874___Accuracy_0.6299999952316284.h5')
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
        file_path = os.path.join(basedir, app.config['UPLOAD_FOLDER'], filename)
        file.save(file_path)
        # print('upload_video filename: ' + filename)
        flash('Video successfully uploaded and displayed below')
        output_video_file_path = f'{prediction_folder}/{filename}.mp4'

        prediction, probability = make_average_predictions(file_path, 10)
        # print(probability)
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


def make_average_predictions(video_file_path, predictions_frames_count):
    # Initializing the Numpy array which will store Prediction Probabilities
    predicted_labels_probabilities_np = np.zeros((predictions_frames_count, model_output_size), dtype=float)

    # Reading the Video File using the VideoCapture Object
    video_reader = cv2.VideoCapture(video_file_path)

    # Getting The Total Frames present in the video
    video_frames_count = int(video_reader.get(cv2.CAP_PROP_FRAME_COUNT))

    # Calculating The Number of Frames to skip Before reading a frame
    skip_frames_window = video_frames_count // predictions_frames_count

    for frame_counter in range(predictions_frames_count):
        # Setting Frame Position
        video_reader.set(cv2.CAP_PROP_POS_FRAMES, frame_counter * skip_frames_window)

        # Reading The Frame
        _, frame = video_reader.read()

        # Resize the Frame to fixed Dimensions
        resized_frame = cv2.resize(frame, (image_height, image_width))

        # Normalize the resized frame by dividing it with 255 so that each pixel value then lies between 0 and 1
        normalized_frame = resized_frame / 255

        # Passing the Image Normalized Frame to the model and receiving Predicted Probabilities.
        predicted_labels_probabilities = model.predict(np.expand_dims(normalized_frame, axis=0))[0]

        # Appending predicted label probabilities to the deque object
        predicted_labels_probabilities_np[frame_counter] = predicted_labels_probabilities

    # Calculating Average of Predicted Labels Probabilities Column Wise
    predicted_labels_probabilities_averaged = predicted_labels_probabilities_np.mean(axis=0)

    # Sorting the Averaged Predicted Labels Probabilities
    predicted_labels_probabilities_averaged_sorted_indexes = np.argsort(predicted_labels_probabilities_averaged)[::-1]

    # Iterating Over All Averaged Predicted Label Probabilities
    for predicted_label in predicted_labels_probabilities_averaged_sorted_indexes:
        # Accessing The Class Name using predicted label.
        predicted_class_name = classes_list[predicted_label]

        # Accessing The Averaged Probability using predicted label.
        predicted_probability = predicted_labels_probabilities_averaged[predicted_label]

        print(f"CLASS NAME: {predicted_class_name}   AVERAGED PROBABILITY: {(predicted_probability * 100)}")

    # Closing the VideoCapture Object and releasing all resources held by it.
    max_index = predicted_labels_probabilities_averaged_sorted_indexes[0]
    max_prediction_label = classes_list[max_index]
    max_prediction_probability = predicted_labels_probabilities_averaged[max_index]
    video_reader.release()
    return [max_prediction_label, max_prediction_probability]

# From juypiter notbook couldnt import, modified for server call

if __name__ == '__main__':
    app.run()
