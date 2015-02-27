module.exports = {
  openPdf: function(url, title, options) {
    cordova.exec(null, function(err) {
      console.log(err);
    }, "MuPdfPlugin", "openPdf", [url, title, options]);
  }
}
