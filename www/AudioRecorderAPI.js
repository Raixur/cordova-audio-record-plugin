function AudioRecorderAPI() {
}

AudioRecorderAPI.prototype.record = function (successCallback, errorCallback, duration) {
  cordova.exec(successCallback, errorCallback, "AudioRecorder", "record", duration ? [duration] : []);
};

AudioRecorderAPI.prototype.stop = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorder", "stop", []);
};

AudioRecorderAPI.prototype.playback = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorder", "playback", []);
};

AudioRecorderAPI.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.audioRecorderAPI = new AudioRecorderAPI();
  return window.plugins.audioRecorderAPI;
};

cordova.addConstructor(AudioRecorderAPI.install);
