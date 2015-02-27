#import <Foundation/Foundation.h>

#include "fitz.h"

@interface MuDocRef : NSObject
{
@public
	fz_document *doc;
	bool interactive;
}
-(id) initWithFilename:(char *)aFilename;
@end
