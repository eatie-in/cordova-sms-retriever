var exec = require("cordova/exec");
const PLUGIN_NAME = "SMSRetriever";

module.exports = {
  getHint:function(){
    return new Promise(function (resolve, reject) {
        exec(resolve, reject, PLUGIN_NAME, "getHint", []);
      });
  },
  getMessage:function(){
    return new Promise(function (resolve, reject) {
        exec(resolve, reject, PLUGIN_NAME, "getMessage", []);
      });
  }
};
