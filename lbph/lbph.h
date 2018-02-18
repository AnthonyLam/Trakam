
#define GET_PIXEL(r, c, pointer) pointer[r][c]
typedef unsigned char uchar;
typedef unsigned int uint;

// lbp calculates the local binary pattern histogram for the given image
// array
//
// uchar** img: A grayscale image array to calculate the lbp for
// uchar** binary: A binary image output from lbp
// uchar*** hist: A binary array of size patch_count * patch_count * 255
// uint rows: Number of rows in img,binary
// uing cols: Number of cols in img,binary
// uing patch_count: Grid size (square)
void lbp(uchar** img, uchar** binary, uchar*** hist, uint rows, uint cols, uint patch_count)
{
    uint patch_size_x = cols / patch_count;
    uint patch_size_y = rows / patch_count;

    // Begin calculating decimal BP values
    for(uint r = 1; r < rows - 1; ++r) {
        for(uint c = 1; c < cols - 1; ++c) {
            uchar threshold = GET_PIXEL(r, c, img);
            // Top row
            GET_PIXEL(r, c, binary) = ((GET_PIXEL(r-1, c-1, img) - threshold) > 0 ? 1 : 0) +
                2 * ((GET_PIXEL(r-1, c, img) - threshold) > 0 ? 1 : 0) +
                4 * ((GET_PIXEL(r-1, c+1, img) - threshold) > 0 ? 1 : 0) +

            // Middle row
                8 * ((GET_PIXEL(r, c-1, img) - threshold) > 0 ? 1 : 0) +
                16 * ((GET_PIXEL(r, c+1, img) - threshold) > 0 ? 1 : 0) +

            // Bottom row
                32 * ((GET_PIXEL(r+1, c-1, img) - threshold) > 0 ? 1 : 0) +
                64 * ((GET_PIXEL(r+1, c, img) - threshold) > 0 ? 1 : 0) +
                128 * ((GET_PIXEL(r+1, c+1, img) - threshold) > 0 ? 1 : 0);
        }
    }

    // Calculate histogram for each region
    for(uint r = 1; r < rows - 1; ++r) {
        for(uint c = 1; c < cols - 1; ++c) {
            hist[r % patch_size_y][c % patch_size_x][GET_PIXEL(r, c, binary)] += 1;
        }
    }
}
