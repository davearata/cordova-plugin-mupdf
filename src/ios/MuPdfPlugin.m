#include "common.h"
#import "MuDocumentController.h"
#import "MuPdfPlugin.h"
#import <Cordova/CDV.h>

@implementation MuPdfPlugin
{
  MuDocRef *doc;
  char *_filePath;
  CDVInvokedUrlCommand* cdvCommand;
}

enum
{
    // use at most 128M for resource cache
    ResourceCacheMaxSize = 128<<20	// use at most 128M for resource cache
};

- (void)pluginInitialize
{
    queue = dispatch_queue_create("com.artifex.mupdf.queue", NULL);

    ctx = fz_new_context(NULL, NULL, ResourceCacheMaxSize);
    fz_register_document_handlers(ctx);
}

- (void)openPdf:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = nil;
  NSString* nspath = [command.arguments objectAtIndex:0];
  NSString* documentTitle = [command.arguments objectAtIndex:1];
  NSDictionary *options = [command argumentAtIndex:2];

  cdvCommand = command;

  if (nspath != nil && [nspath length] > 0) {
    [self openDocument:nspath title:documentTitle options:options];
  } else {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
}

- (void) openDocument: (NSString*)nspath title:(NSString*)documentTitle options:(NSDictionary*)options
{
  _filePath = malloc(strlen([nspath UTF8String])+1);
  if (_filePath == NULL) {
    printf("Out of memory in openDocument");
    return;
  }

  strcpy(_filePath, [nspath UTF8String]);

  dispatch_sync(queue, ^{});

  printf("open document '%s'\n", _filePath);

  doc = [[MuDocRef alloc] initWithFilename:_filePath];
  if (!doc) {
    printf("Cannot open document");
    return;
  }

  MuDocumentController *document = [[MuDocumentController alloc] initWithFilename: documentTitle path:_filePath document:doc options:options];
  if (document) {
    UINavigationController* navigationController = [[UINavigationController alloc] initWithRootViewController:document];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                          selector:@selector(didDismissDocumentController:)
                                          name:@"DocumentControllerDismissed"
                                          object:nil];
    [self.viewController presentViewController:navigationController animated:YES completion:nil];
  }
  free(_filePath);
}

-(void)didDismissDocumentController:(NSNotification *)notification {
  NSDictionary* saveResults = [notification object];
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:saveResults];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:cdvCommand.callbackId];
}

@end
