#ifndef MUPDF_FITZ_OUTPUT_TGA_H
#define MUPDF_FITZ_OUTPUT_TGA_H

#include "system.h"
#include "context.h"
#include "pixmap.h"

void fz_save_pixmap_as_tga(fz_context *ctx, fz_pixmap *pixmap, const char *filename, int savealpha);
void fz_write_pixmap_as_tga(fz_context *ctx, fz_output *out, fz_pixmap *pixmap, int savealpha);

#endif
