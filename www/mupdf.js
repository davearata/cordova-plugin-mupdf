module.exports = {
  openPdf: function(url, title, headerColor) {
    cordova.exec(null, null, "MuPdfPlugin", "openPdf", [url, title, headerColor]);
  }
}
