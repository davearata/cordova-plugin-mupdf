module.exports = {
  openPdf: function(url, title, headerColor) {
    cordova.exec(null, function(err) {
      console.log(err);
    }, "MuPdfPlugin", "openPdf", [url, title, headerColor]);
  }
}
