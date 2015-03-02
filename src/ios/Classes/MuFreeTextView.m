#include "common.h"
#import "MuFreeTextView.h"

@implementation MuFreeTextView
{
	CGSize pageSize;
}

- (id) initWithPageSize:(CGSize)_pageSize
{
	self = [super initWithFrame:CGRectMake(0, 0, 100, 100)];
	if (self) {
		[self setOpaque:NO];
		pageSize = _pageSize;
		UITapGestureRecognizer *rec = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(onTap:)];
		[self addGestureRecognizer:rec];
		[rec release];
	}
	return self;
}

-(void) onTap:(UITapGestureRecognizer *)rec
{
	CGSize scale = fitPageToScreen(pageSize, self.bounds.size);
	CGPoint p = [rec locationInView:self];
	p.x /= scale.width;
	p.y /= scale.height;

  UIImageView *annotationImage =[[UIImageView alloc] initWithFrame:CGRectMake(p.x, p.y,20,20)];
  annotationImage.image=[UIImage imageNamed:@"ic_free_text.png"];
  [self addSubview:annotationImage];

	[self setNeedsDisplay];
}

- (void)drawRect:(CGRect)rect
{
	CGSize scale = fitPageToScreen(pageSize, self.bounds.size);
	CGContextRef cref = UIGraphicsGetCurrentContext();
	CGContextScaleCTM(cref, scale.width, scale.height);
}

@end
