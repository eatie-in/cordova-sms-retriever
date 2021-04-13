var exec = require("cordova/exec");
const PLUGIN_NAME = "SMSRetriever";

module.exports = {
  getHint: function () {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, PLUGIN_NAME, "getHint", []);
    });
  },
  getSMS(options,successCallback, errorCallback) {
    exec(successCallback, errorCallback, PLUGIN_NAME, "getSMS", [options]);
  },
  getAppHash() {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, PLUGIN_NAME, "getAppHash", []);
    });
  },
};
