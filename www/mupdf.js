module.exports = {
  openPdf: function(url, title, options, dismissCallback) {
    cordova.exec(dismissCallback, function(err) {
      console.log(err);
    }, "MuPdfPlugin", "openPdf", [url, title, options]);
  }
};
