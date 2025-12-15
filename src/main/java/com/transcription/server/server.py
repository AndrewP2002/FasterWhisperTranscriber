from flask import Flask, request, jsonify
import os
import subprocess
from ollama_translate import translate_srt
import srt

app = Flask(__name__)

@app.route('/api/transcribe', methods=['POST'])
def transcribe():
    try:
        #All the parameters from the user
        audio_file = request.files['file']
        language = "--language " + request.form.get('language')
        model = "--model " + request.form.get('model')
        translate_check = request.form.get('translate') == 'true'
        target_language = request.form.get('target_language')    

        #current dirrectory
        script_path = os.path.abspath(__file__)
        script_directory = os.path.dirname(script_path)

        #saving the file in the same dir
        file_path = os.path.join(script_directory, audio_file.filename)
        audio_file.save(file_path)
        whisper_folder = os.path.join(script_directory,"Faster-Whisper-XXL")
        whisper_file = os.path.join(whisper_folder, "faster-whisper-xxl.exe")

        #command for whisper
        command = whisper_file + " " + file_path + " " + language + " " + model + " --output_dir source"

        #using faster-whisper
        subprocess.run(command, check = True, capture_output=True, text=True)
        srt_path = file_path[:-4]+".srt"
        with open(srt_path,'r',encoding='utf-8') as f:
            original_subs = list(srt.parse(f.read()))

        #convert the srt file into text
        original_subs_result = srt.compose(original_subs)

        #ollama translation of the created srt file
        if translate_check:
            translated_subs_result = translate_srt(srt_path, target_language)
        os.remove(file_path)
        os.remove(srt_path)
        #returning the texts
        if translate_check:
            return jsonify({
                'srt_original': original_subs_result,
                'srt_translated': translated_subs_result
            })
        else:
             return jsonify({
                'srt_original': original_subs_result
             })
    except Exception as e:
        return str(e), 500
    
if __name__ == '__main__':
    app.run(debug=True, port=8080)