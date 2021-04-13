## Installation

    $ cordova plugin add

## Methods

### getHint(_type_)

to prompt the user to choose from the phone numbers stored on the device

```js
cordova.plugins.SMSRetriever.getHint()
  .then((hint) => {
    console.log(hint);
  })
  .catch(console.log);
```

### getAppHash(_type_)

gets an 11-character hash string that identifies your app

```js
cordova.plugins.SMSRetriever.getHint()
  .then((hint) => {
    console.log(hint);
  })
  .catch(console.error);
```

### getSMS(_callback_)

call to begin listening for an SMS message
```js
// Default uses SMS Retriever API,to use Consent API pass
const options = {
    consent:true
}
cordova.plugins.SMSRetriever.getSMS(options
  (sms) => {
    // sms
    console.log(sms);
  },
  (error) => {
    console.log(error); // timeout error
  }
);
```
