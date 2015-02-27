#include "common.h"
#include "pdf.h"
#import <Foundation/Foundation.h>

@interface MuAnnotation : NSObject
-(id) initFromAnnot:(fz_annot *)annot forPage:(fz_page *)page;
@property(readonly) int type;
@property(readonly) CGRect rect;
+(MuAnnotation *) annotFromAnnot:(fz_annot *)annot forPage:(fz_page *)page;
@end
