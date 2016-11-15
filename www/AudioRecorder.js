function AudioRecorder() {
}

AudioRecorder.prototype.record = function (successCallback, errorCallback, fileDir) {
  cordova.exec(successCallback, errorCallback, "AudioRecorder", "record", fileDir ? [fileDir] : []);
};

AudioRecorder.prototype.stop = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorder", "stop", []);
};

AudioRecorder.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.audioRecorder = new AudioRecorder();
  return window.plugins.audioRecorder;
};

cordova.addConstructor(AudioRecorder.install);
