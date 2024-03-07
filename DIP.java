import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.HashMap;

public class DIP{
	public static final int RED_CHANNEL = 0;
	public static final int GREEN_CHANNEL = 1;
	public static final int BLUE_CHANNEL = 2;
	
	public int width = 0;
	public int height = 0;
	public int type = 0;
	public String extension = null;
	
	// WOULD BE BETTER IF THESE VALUES ARE PACKED INTO ARRAYS AND THE LOGIC IS ADJUSTED ACCORDINGLY (currently not that important)
	float[] histR = null;
	float[] histG = null;
	float[] histB = null;
	float meanR = -1;
	float meanG = -1;
	float meanB = -1;
	float varianceR = -1;
	float varianceG = -1;
	float varianceB = -1;
	
	public BufferedImage originalImage = null;
	public BufferedImage supportImage = null;
	
	public BufferedImage cachedOriginalImage = null;
	public BufferedImage cachedSupportImage = null;
	
	// NOT USED
	public ArrayList<BufferedImage> supportImagesList = null;
	
	public DIP(){}
	
	// Loads original image and sets support image to empty buffer with corresponding width, height and type
	public void loadImage(File imageFile){
		try{
			originalImage = ImageIO.read(imageFile);
			
			width = originalImage.getWidth();
			height = originalImage.getHeight();
			type = originalImage.getType();
			extension = getFileExtension(imageFile);
			
			supportImage = new BufferedImage(width, height, type);
		}
		catch(IOException e){
			System.out.println("ERROR::Loading image");
		}
	}
	
	// Saves original image
	public void saveOriginalImage(String fileName){
		try{
			ImageIO.write(originalImage, extension, new File(fileName + "." + extension));
		}
		catch(IOException e){
			System.out.println("ERROR::Saving image");
		}
	}
	
	// Saves support image
	public void saveSupportImage(String fileName){
		try{
			ImageIO.write(supportImage, extension, new File(fileName + "." + extension));
		}
		catch(IOException e){
			System.out.println("ERROR::Saving image");
		}
	}
	
	// Sets original image. Additionally can set basic info (width,height,type) if second arg is true
	public void setOriginalImage(File imageFile, boolean setBasicInfo){
		try{
			originalImage = ImageIO.read(imageFile);
			
			if(setBasicInfo){
				width = originalImage.getWidth();
				height = originalImage.getHeight();
				type = originalImage.getType();
				extension = getFileExtension(imageFile);
			}
		}
		catch(IOException e){
			System.out.println("ERROR::Loading image");
		}
	}
	
	// Sets support image. Additionally can set basic info (width,height,type) if second arg is true
	public void setSupportImage(File imageFile, boolean setBasicInfo){
		try{
			supportImage = ImageIO.read(imageFile);
			
			if(setBasicInfo){
				width = supportImage.getWidth();
				height = supportImage.getHeight();
				type = supportImage.getType();
				extension = getFileExtension(imageFile);
			}
		}
		catch(IOException e){
			System.out.println("ERROR::Loading image");
		}
	}
	
	// Requires established originalImage
	public void originalImageFrom(Pixel[][] pixels){
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				if(pixels[i][j] == null) originalImage.setRGB(j, i, 0xff000000);
				
				originalImage.setRGB(j, i, pixels[i][j].toInt());
			}
		}
	}
	
	// Requires established supportImage
	public void supportImageFrom(Pixel[][] pixels){
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				if(pixels[i][j] == null) supportImage.setRGB(j, i, 0xff000000);
				
				supportImage.setRGB(j, i, pixels[i][j].toInt());
			}
		}
	}
	
	// Shifts support image to original image and destroys support image (done by pointer switch)
	public void shift(){
		originalImage = supportImage;
		supportImage = new BufferedImage(width, height, type);
		
		// Set these values to null because now we have new base image
		histR = null;
		histG = null;
		histB = null;
		meanR = -1;
		meanG = -1;
		meanB = -1;
		varianceR = -1;
		varianceG = -1;
		varianceB = -1;
	}
	
	// Switches original and support image (done by pointer switch)
	public void switchImages(){
		BufferedImage temp = originalImage;
		originalImage = supportImage;
		supportImage = temp;
	}
	
	public void grayscale(){
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				supportImage.setRGB(j, i, p.toGrayscaleInt());
			}
		}
	}
	
	public void blur3(){
		Pixel p = new Pixel();
		for(int i = 1; i < height - 1; ++i){
			for(int j = 1; j < width - 1; ++j){
				p.setPixel(0xff000000);
				
				for(int k = -1; k <= 1; ++k)
					for(int l = -1; l <= 1; ++l)
						p.addElementWise(originalImage.getRGB(j + l, i + k));
				
				p.scale(1.0f/9.0f);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void median3(int channel){
		Pixel p = new Pixel();
		
		// Holds chosen channel values for pixels that will be sorted to find median
		int[] values = new int[9];
		int[] temp = null;
		
		for(int i = 1; i < height - 1; ++i){
			for(int j = 1; j < width - 1; ++j){
				
				for(int k = -1; k <= 1; ++k){
					for(int l = -1; l <= 1; ++l){
						// Get pixel's channel value and place it into the array
						p.setPixel(originalImage.getRGB(j + l, i + k));
						values[(k+1)*3 + (l+1)] = p.getChannel(channel);
					}
				}
				
				// Remove same elements before taking the middle one
				temp = Arrays.stream(values).distinct().toArray();
				
				// Do the sorting
				Arrays.sort(temp);
				
				// Set the value of the pixel to median, which is the middle of sorted array
				p.setChannel(channel, temp[temp.length / 2]);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void median(int channel, int kernelSize){
		if(kernelSize <= 0)
			return;
		
		if(kernelSize % 2 == 0)
			++kernelSize;
		
		Pixel p = new Pixel();
		
		// Holds chosen channel values for pixels that will be sorted to find median
		int[] values = new int[kernelSize*kernelSize];
		int[] temp = null;
		
		int kernelMiddle = (int)Math.floor(kernelSize / 2.0);
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				
				for(int k = -kernelMiddle; k <= kernelMiddle; ++k){
					for(int l = -kernelMiddle; l <= kernelMiddle; ++l){
						if(!checkPixelLocation(j + l,i + k)){
							values[(k+kernelMiddle)*kernelSize + (l+kernelMiddle)] = 0;
							continue;
						}
						
						// Get pixel's channel value and place it into the array
						p.setPixel(originalImage.getRGB(j + l, i + k));
						values[(k+kernelMiddle)*kernelSize + (l+kernelMiddle)] = p.getChannel(channel);
					}
				}
				
				// Remove same elements before taking the middle one
				temp = Arrays.stream(values).distinct().toArray();
				
				// Do the sorting
				Arrays.sort(temp);
				
				// Set the value of the pixel to median, which is the middle of sorted array
				p.setChannel(channel, temp[temp.length / 2]);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void blur(int kernelWidth, int kernelHeight){
		int widthOffset = (int)Math.floor(kernelWidth / 2.0f);
		int heightOffset = (int)Math.floor(kernelHeight / 2.0f);
		
		int widthUpperEdge = kernelWidth - widthOffset;
		int heightUpperEdge = kernelHeight - heightOffset;
		
		float scaleFactor = 1.0f/(kernelWidth * kernelHeight);
		
		Pixel p = new Pixel();
		int JL = 0;
		int IK = 0;
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(0xff000000);
				
				for(int k = -heightOffset; k < heightUpperEdge; ++k){
					for(int l = -widthOffset; l < widthUpperEdge; ++l){
						JL = j + l;
						IK = i + k;
						if(checkPixelLocation(JL, IK))
							p.addElementWise(originalImage.getRGB(JL, IK));
					}
				}
				
				p.scale(scaleFactor);
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void negative(int pivotValue){
		Pixel pivot = new Pixel(pivotValue);
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				supportImage.setRGB(j, i, Pixel.clampElementWise(Pixel.subElementWise(pivot, p), 0, 255).toInt());
			}
		}
	}
	
	public void negativeWithoutClamp(int pivotValue){
		Pixel pivot = new Pixel(pivotValue);
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				supportImage.setRGB(j, i, Pixel.subElementWise(pivot, p).toInt());
			}
		}
	}
	
	// Does it for all channels (uses clamp)
	public void derivativeX(){
		Pixel current = new Pixel();
		Pixel next = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				current.setPixel(originalImage.getRGB(j, i));
				
				if(j + 1 >= width){
					supportImage.setRGB(j, i, current.toInt());
					continue;
				}
				
				next.setPixel(originalImage.getRGB(j+1, i));
				
				current = Pixel.subElementWise(next, current);
				current.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, current.toInt());
			}
		}
	}
	
	// Does it for all channels (uses clamp)
	public void derivativeY(){
		Pixel current = new Pixel();
		Pixel next = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				current.setPixel(originalImage.getRGB(j, i));
				
				if(i + 1 >= height){
					supportImage.setRGB(j, i, current.toInt());
					continue;
				}
				
				next.setPixel(originalImage.getRGB(j, i+1));
				
				current = Pixel.subElementWise(next, current);
				current.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, current.toInt());
			}
		}
	}
	
	// 0  1  0
	// 1 -4  1
	// 0  1  0
	// For all channels
	// It also returns pixel matrix where pixels are not clamped and are not interpolated
	public Pixel[][] laplacianBasic(){
		int[][] mat = { {0, -1, 0}, {-1, 4, -1}, {0, -1, 0}};
		
		// This is to save pixels with negative values before fitting them to interval [0-255] by interpolation
		Pixel[][] imgMat = new Pixel[height][width];
		// These are in order to do interpolation
		Pixel min = new Pixel();
		Pixel max = new Pixel();
		
		Pixel p = new Pixel();
		Pixel temp = new Pixel();
		
		for(int i = 1; i < height - 1; ++i){
			for(int j = 1; j < width - 1; ++j){
				p.setPixel(0xff000000);
				
				for(int k = -1; k <= 1; ++k){
					for(int l = -1; l <= 1; ++l){
						temp.setPixel(originalImage.getRGB(j + l, i + k));
						temp.scale(mat[k+1][l+1]);
						
						p.addElementWise(temp);
					}
				}
				
				imgMat[i][j] = new Pixel(255, p.r, p.g, p.b);
				
				// For finding minimum channel values
				if(p.r < min.r) min.r = p.r;
				if(p.g < min.g) min.g = p.g;
				if(p.b < min.b) min.b = p.b;
				
				// For finding maximum channel values
				if(p.r > max.r) max.r = p.r;
				if(p.g > max.g) max.g = p.g;
				if(p.b > max.b) max.b = p.b;
			}
		}
		
		// More standard way is to subtract -128 instead of min value and then clamp all values
		// This is because it is faster since there is no need to go over all pixels and look for one with minimal value
		
		float sR = 1.0f / (max.r - min.r) * 255;
		float sG = 1.0f / (max.g - min.g) * 255;
		float sB = 1.0f / (max.b - min.b) * 255;
		
		for(int i = 1; i < height - 1; ++i){
			for(int j = 1; j < width - 1; ++j){
				p.r = imgMat[i][j].r;
				p.g = imgMat[i][j].g;
				p.b = imgMat[i][j].b;
				
				p.subElementWise(min);
				p.scale(sR, sG, sB);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
		
		return imgMat;
	}
	
	// This uses unsharp masking where we take original image, blur it and find the difference between the two
	// Then, we add that difference scaled by some factor 'k' to the original image
	// Simple, average blur is used here instead of Gaussian
	// For all channels
	// NOT WORKING
	public void highboostFilter(int blurKernelSize, float k){
		if(blurKernelSize <= 0) return;
		if(blurKernelSize % 2 == 0) ++blurKernelSize;
		
		// Make blur image and place it into support image
		blur(blurKernelSize, blurKernelSize);
		
		// Make difference image and place it into support image
		Pixel[][] imgMat = sub();
		Pixel[] minMax = getMinMaxPixel(imgMat);
		//interpolateToStandardInterval(imgMat, minMax);
		supportImageFrom(imgMat);
		saveSupportImage("images/XXXX_diff");
		
		// Shift support to original image, enhance brightness, and place the result to support image
		cachedOriginalImage = originalImage;
		switchImages();
		enhanceBrightnessLinear(k);
		originalImage = cachedOriginalImage;
		
		// Add original image and enhanced difference image and place it to support image
		imgMat = add();
		interpolateToStandardInterval(imgMat,minMax);
		supportImageFrom(imgMat);
	}
	
	// Adds original and support image and places result in support
	// Additionally returns pixel matrix that doesn't necessarily lie in [0,255] range
	public Pixel[][] add(){
		Pixel[][] imgMat = new Pixel[height][width];
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				p.addElementWise(supportImage.getRGB(j, i));
				
				imgMat[i][j] = new Pixel(255, p.r, p.g, p.b);
				
				supportImage.setRGB(j, i, Pixel.addElementWise(originalImage.getRGB(j, i), supportImage.getRGB(j, i)).toInt());
			}
		}
		
		return imgMat;
	}
	
	// Subtracts support image from original image
	// Additionally returns pixel matrix that doesn't necessarily lie in [0,255] range
	public Pixel[][] sub(){
		Pixel[][] imgMat = new Pixel[height][width];
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				p.subElementWise(supportImage.getRGB(j, i));
				
				imgMat[i][j] = new Pixel(255, p.r, p.g, p.b);
				
				supportImage.setRGB(j, i, Pixel.subElementWise(originalImage.getRGB(j, i), supportImage.getRGB(j, i)).toInt());
			}
		}
		
		return imgMat;
	}
	
	// Adds original image and given pixel matrix and places result in support image
	public void add(Pixel[][] pixels){
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				if(pixels[i][j] != null)
					supportImage.setRGB(j, i, Pixel.addElementWise(p, pixels[i][j]).toInt());
				else
					supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// Subtracts pixels from original image
	public void sub(Pixel[][] pixels){
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				if(pixels[i][j] != null)
					supportImage.setRGB(j, i, Pixel.subElementWise(p, pixels[i][j]).toInt());
				else
					supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// Nonstandard contrast enhancement
	// Example with 2 new levels: 0------128------256
	// Those between 0 and 128 get mapped to one or the other depending on closeness (same on the other side)
	// Result is then: [0---][---128---][---256] meaning that most of the values get mapped to middle value
	// piecewiseLinearTransform(...) should be used for standard way of defining contrast
	public void enhanceContrastAll(int nLevelsOrig, int nLevelsNew){
		int levelWidth = (int)Math.floor((float)nLevelsOrig / nLevelsNew);
		float levelHalfWidth = (levelWidth / 2.0f);
		
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				p.r = ((p.r % levelWidth) > levelHalfWidth) ? ((p.r / levelWidth) + 1) * levelWidth : (p.r / levelWidth) * levelWidth;
				p.g = ((p.g % levelWidth) > levelHalfWidth) ? ((p.g / levelWidth) + 1) * levelWidth : (p.g / levelWidth) * levelWidth;
				p.b = ((p.b % levelWidth) > levelHalfWidth) ? ((p.b / levelWidth) + 1) * levelWidth : (p.b / levelWidth) * levelWidth;
				
				p.clampElementWise(0, nLevelsOrig - 1);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void enhanceContrastPerChannel(int nLOrigR, int nLOrigG, int nLOrigB, int nLNewR, int nLNewG, int nLNewB){
		int lWidthR = (int)Math.floor((float)nLOrigR / nLNewR);
		float lHalfWidthR = (lWidthR / 2.0f);
		int lWidthG = (int)Math.floor((float)nLOrigG / nLNewG);
		float lHalfWidthG = (lWidthG / 2.0f);
		int lWidthB = (int)Math.floor((float)nLOrigB / nLNewB);
		float lHalfWidthB = (lWidthB / 2.0f);
		
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				p.r = (p.r % lWidthR > lHalfWidthR) ? ((p.r / lWidthR) + 1) * lWidthR : (p.r / lWidthR) * lWidthR;
				p.g = (p.g % lWidthG > lHalfWidthG) ? ((p.g / lWidthG) + 1) * lWidthG : (p.g / lWidthG) * lWidthG;
				p.b = (p.b % lWidthB > lHalfWidthB) ? ((p.b / lWidthB) + 1) * lWidthB : (p.b / lWidthB) * lWidthB;
				
				p.clampRed(0, nLOrigR - 1);
				p.clampGreen(0, nLOrigG - 1);
				p.clampBlue(0, nLOrigB - 1);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public void enhanceBrightnessLinear(float factor){
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				p.scale(factor);
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// s = c*log(1+r)
	public void intensityLogTransform(float c){
		Pixel p = new Pixel();
		double logTransformValuePerChannel = 0.0;
		double maxIntensityLog = Math.log(256);
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				logTransformValuePerChannel = c * Math.log(1 + p.r);
				p.r = (int)Math.round((logTransformValuePerChannel / maxIntensityLog) * 255);
				
				logTransformValuePerChannel = c * Math.log(1 + p.g);
				p.g = (int)Math.round((logTransformValuePerChannel / maxIntensityLog) * 255);
				
				logTransformValuePerChannel = c * Math.log(1 + p.b);
				p.b = (int)Math.round((logTransformValuePerChannel / maxIntensityLog) * 255);
				
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// s = c*r^gamma (can also have (r + epsilon) instead of r in order to have non-zero input when r=0)
	public void intensityGammaTransform(float c, float gamma){
		Pixel p = new Pixel();
		
		float nr = 0.0f, ng = 0.0f, nb = 0.0f;
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				nr = Pixel.normalize(p.r, 255);
				ng = Pixel.normalize(p.g, 255);
				nb = Pixel.normalize(p.b, 255);
				
				nr = c * (float)Math.pow(nr, gamma);
				ng = c * (float)Math.pow(ng, gamma);
				nb = c * (float)Math.pow(nb, gamma);
				
				p.r = Pixel.denormalize(Pixel.clamp(nr, 0.0f, 1.0f), 255);
				p.g = Pixel.denormalize(Pixel.clamp(ng, 0.0f, 1.0f), 255);
				p.b = Pixel.denormalize(Pixel.clamp(nb, 0.0f, 1.0f), 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// THIS CAN BE USED AS A GENERAL TOOL WHEN THERE IS A NEED TO APPLY PIECEWISE LINEAR FUNCTION (intensity slicing, bit plane slicing, countour maps,...)
	// Assumption is that the order is r1,s1,r2,s2,... and that the number of arguments is even
	// r1 <= r2 <= ... needs to hold (monotonically increasing)
	// At least two points need to be defined
	public void piecewiseLinearTransform(Integer... coords){
		Pixel p = new Pixel();
		
		HashMap<Integer, Function<Integer, Float>> functions = getPiecewiseLinearTable(coords);

		int redBorderIndex = -1;
		int greenBorderIndex = -1;
		int blueBorderIndex = -1;
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				redBorderIndex = -1;
				greenBorderIndex = -1;
				blueBorderIndex = -1;
		
				p.setPixel(originalImage.getRGB(j, i));
				
				// Go through r values to find the right linear function index
				for(int k = 0; k < coords.length; k+=2){
					if(redBorderIndex == -1 && p.r <= coords[k])
						redBorderIndex = k-2;
					if(greenBorderIndex == -1 && p.g <= coords[k])
						greenBorderIndex = k-2;
					if(blueBorderIndex == -1 && p.b <= coords[k])
						blueBorderIndex = k-2;
				}
				
				// Get linear function for each channel, handle edge case and perform the mapping
				p.r = Math.round(functions.get(redBorderIndex < 0 ? 0 : redBorderIndex).apply(p.r));
				p.g = Math.round(functions.get(greenBorderIndex < 0 ? 0 : greenBorderIndex).apply(p.g));
				p.b = Math.round(functions.get(blueBorderIndex < 0 ? 0 : blueBorderIndex).apply(p.b));
				
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// Only makes sense for values 1,...,8 since there are that many bits (excluding images with 16bit)
	// Can be used for compression because we can for example extract 8th and 7th plane (or more) and just encode those bits
	// That would be compression with losses
	public void bitPlane(int n){
		Pixel p = new Pixel();
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				p.r = (((p.r >>> (n-1)) & 0x1) == 1) ? 255 : 0;
				p.g = (((p.g >>> (n-1)) & 0x1) == 1) ? 255 : 0;
				p.b = (((p.b >>> (n-1)) & 0x1) == 1) ? 255 : 0;
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	public float[] histogram(int channel){
		// If histogram has previously been calculated for selected channel of the current image, then just return it
		if(channel == RED_CHANNEL && histR != null) return histR;
		if(channel == GREEN_CHANNEL && histG != null) return histG;
		if(channel == BLUE_CHANNEL && histB != null) return histB;
		 
		float[] hist = new float[256];
		
		Pixel p = new Pixel();
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				hist[p.getChannel(channel)]++;
			}
		}
		
		// Normalization
		int numPixels = width * height;
		for(int i = 0; i < 256; ++i)
			hist[i] /= numPixels;
		
		// Set histogram for specified channel for current image
		if(channel == RED_CHANNEL) histR = hist;
		if(channel == GREEN_CHANNEL) histG = hist;
		if(channel == BLUE_CHANNEL) histB = hist;
		return hist;
	}
	
	public float[] localHistogram(int channel, int kernelWidth, int kernelHeight, int j, int i){
		int widthOffset = (int)Math.floor(kernelWidth / 2.0f);
		int heightOffset = (int)Math.floor(kernelHeight / 2.0f);
		
		int widthUpperEdge = kernelWidth - widthOffset;
		int heightUpperEdge = kernelHeight - heightOffset;
		
		// Histogram of local intensities within the block of size kernelWidth x kernelHeight
		// Obviously, we could limit this size, or even request array to be passed as function parameter for the sake of optimization
		float[] localHist = new float[256];
		
		// Loop through defined block size and fill up local histogram
		Pixel p = new Pixel();
		int JL = 0;
		int IK = 0;
		for(int k = -heightOffset; k < heightUpperEdge; ++k){
			for(int l = -widthOffset; l < widthUpperEdge; ++l){
				JL = j + l;
				IK = i + k;
				
				if(checkPixelLocation(JL, IK)){
					p.setPixel(originalImage.getRGB(JL, IK));
					localHist[p.getChannel(channel)]++;
				}
			}
		}
		
		// Normalization
		int numPixels = kernelWidth * kernelHeight;
		for(int r = 0; r < 256; ++r)
			localHist[r] /= numPixels;
		
		return localHist;
	}
	
	// Plot normalized histogram given its normalized values
	// It is better to use ".png" extension than ".jpg" because of lossless compression
	public void plotHistogram(File plotFile, float[] hist, float scale){
		try{
			BufferedImage histPlot = new BufferedImage(256, 256, type);
			
			float frequencyWidth = 0.0f;
			
			int color = 0xff0000ff;
			
			for(int i = 0; i < 256; ++i){
				frequencyWidth = (hist[i] * 100) * scale;
				color ^= 0x00999900;
				
				for(int j = 0; j < 256; ++j){
					if(j <= frequencyWidth)
						histPlot.setRGB(j, i, color);
					else
						histPlot.setRGB(j, i, 0xffffffff);
				}
			}
			
			ImageIO.write(histPlot, getFileExtension(plotFile), plotFile);
		}
		catch(Exception e){
			System.out.println("ERROR::Plotting histogram");
			e.printStackTrace();
		}
	}
	
	// Plot normalized histogram for chosen channel of currently loaded image
	// It is better to use ".png" extension than ".jpg" because of lossless compression
	public void plotImageHistogram(File histImageFile, int channel, float scale){
		try{
			float[] hist = histogram(channel);
			
			BufferedImage histImage = new BufferedImage(256, 256, type);
			
			float frequencyWidth = 0.0f;
			
			int color = 0xff0000ff;
			
			for(int i = 0; i < 256; ++i){
				frequencyWidth = (hist[i] * 100) * scale;
				color ^= 0x00999900;
				
				for(int j = 0; j < 256; ++j){
					if(j <= frequencyWidth)
						histImage.setRGB(j, i, color);
					else
						histImage.setRGB(j, i, 0xffffffff);
				}
			}
			
			ImageIO.write(histImage, getFileExtension(histImageFile), histImageFile);
		}
		catch(Exception e){
			System.out.println("ERROR::Plotting histogram image");
			e.printStackTrace();
		}
	}
	
	// THIS PROCESS IS AUTOMATIC, MEANING THAT IT IS WELL DEFINED ON ITS OWN AND DOES NOT NEED ANY ADITIONAL DATA
	// IF THERE IS A NEED FOR DEFINING CUSTOM HISTOGRAM FORM FOR MODIFIED IMAGE, THEN ANOTHER PROCESS MUST BE USED
	// Assumption is that there are 256 intensity levels
	// Formula used: s = T(r) = (L-1)*INTEGRAL[from 0 to r](pr(w)dw), pr(r) is PDF for r, L is 256
	// Discrete case uses sum (from 0 to k) instead of integral and Sk and Rk as discrete points instead of s and r
	// In practice, it is rare that the histogram after application is totally flat (since we approximate integral)
	// However, intensty values are closer and are more spread, resulting in higher contrast
	public void histogramEqualization(int channel){
		Pixel p = new Pixel();
		
		float[] hist = histogram(channel);
		
		// Define mapping T(r)=s
		// Index is value of r and value is value of s
		float[] sValues = new float[256];
		float sum = 0;
		for(int i = 0; i < 256; ++i){
			sum = 0;
			for(int j = 0; j <= i; ++j){
				sum += hist[j];
			}
			sValues[i] = sum;
		}
		
		int r = 0;
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				r = p.getChannel(channel);
				
				p.setChannel(channel, Math.round(sValues[r] * 255));
				
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// Map pixel intensities is such a way that PDF of input image tends to PDF given by customHist (chosen normalized histogram)
	public void histogramMatching(int channel, float[] customHist){
		float sum = 0.0f;
		
		// PDF of random variable r
		float[] hist = histogram(channel);
		
		// Holds values of s that we get by mapping r
		// Index is r, value if T(r)=s
		int[] TofR = new int[256];
		for(int i = 0; i < 256; ++i){
			sum = 0.0f;
			for(int j = 0; j <= i; ++j)
				sum += hist[j];
			
			TofR[i] = Math.round(sum * 255);
		}
		
		// Index defines value of z, and value defines value of G(z)=s
		// z is random variable with 'customHist' PDF
		// s is random variable with uniform PDF
		List<Integer> GofZ = new ArrayList<>();
		
		// G(z)=(L-1)*INTEGRAL[from 0 to q][of pz(z)]
		// We use discrete case of this formula where we have sum instead of integral, L is 256, and pz is equal to 'customHist'
		for(int i = 0; i < 256; ++i){
			sum = 0.0f;
			for(int j = 0; j <= i; ++j)
				sum += customHist[j];
			
			GofZ.add(Math.round(sum * 255));
		}
		
		
		int[] rToZ = new int[256];
		int closestValueIndex = 0;
		int minDiff = 255;
		Integer z = null;
		for(int i = 0; i < 256; ++i){
			z = GofZ.indexOf(TofR[i]);
			
			// If there is a value G(z) such that G(z)=T(r) then save that mapping
			if(z != -1){
				rToZ[i] = z;
			}
			// If the value does not exist, then find the value of z for which we have the closest value of G(z) to current T(r)
			else{
				int absDiff = 0;
				for(int j = 0; j < 256; ++j){
					absDiff = Math.abs(GofZ.get(j) - TofR[i]);
					
					if(absDiff < minDiff){
						minDiff = absDiff;
						closestValueIndex = j;
					}
				}
				
				rToZ[i] = closestValueIndex;
			}
		}
		
		Pixel p = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				p.getChannel(channel);
				
				p.setChannel(channel, rToZ[p.getChannel(channel)]);
				
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}
	
	// Does local histogram equalization over the block given by kernelWidth x kernelHeight
	// Histogram is created based on the size of the block and only central pixel is altered after moving further
	// Optimization would be just updating previously computed histogram when kernel is moved by one place (instead of calculating
	// new one all over again)
	// CAN GIVE NICE CIRCUIT BOARD EFFECT (for lower kernel dimensions and higher resolution images) :D
	// GIVES "crystalized" effect
	public void localHistogramEqualization(int channel, int kernelWidth, int kernelHeight){
		Pixel p = new Pixel();
		
		float[] localHist = null;
		
		int r = 0;
		float sum = 0.0f;
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				sum = 0;
				localHist = localHistogram(channel, kernelWidth, kernelHeight, j, i);
				
				p.setPixel(originalImage.getRGB(j, i));
				r = p.getChannel(channel);
				
				for(int k = 0; k <= r; ++k)
					sum += localHist[k];
				
				p.setChannel(channel, Math.round(sum * 255));
				
				p.clampElementWise(0, 255);
				
				supportImage.setRGB(j, i, p.toInt());
			}
		}
	}

	public float meanIntensityAll(){
		Pixel p = new Pixel();
		
		float mean = 0.0f;
		
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				mean += (p.r + p.g + p.b) / 3.0f;
			}
		}
		
		return mean / (width * height);
	}
	
	public float meanIntensity(int channel){
		// If mean has previously been calculated for selected channel of the current image, then just return it
		if(channel == RED_CHANNEL && meanR != -1) return meanR;
		if(channel == GREEN_CHANNEL && meanG != -1) return meanG;
		if(channel == BLUE_CHANNEL && meanB != -1) return meanB;
		
		float[] hist = histogram(channel);
		// Actually, it's expected value
		float mean = 0.0f;
		
		for(int r = 0; r < hist.length; ++r)
			mean += r * hist[r];
		
		if(channel == RED_CHANNEL) meanR = mean;
		if(channel == GREEN_CHANNEL) meanG = mean;
		if(channel == BLUE_CHANNEL) meanB = mean;
		return mean;
	}
	
	public float intensityVariance(int channel){
		// If variance has previously been calculated for selected channel of the current image, then just return it
		if(channel == RED_CHANNEL && varianceR != -1) return varianceR;
		if(channel == GREEN_CHANNEL && varianceG != -1) return varianceG;
		if(channel == BLUE_CHANNEL && varianceB != -1) return varianceB;
		
		float[] hist = histogram(channel);
		float mean = meanIntensity(channel);
		
		float variance = 0.0f;
		
		for(int r = 0; r < hist.length; ++r)
			variance += (r-mean)*(r-mean)*hist[r];
		
		if(channel == RED_CHANNEL) varianceR = variance;
		if(channel == GREEN_CHANNEL) varianceG = variance;
		if(channel == BLUE_CHANNEL) varianceB = variance;
		return variance;
	}
	
	public float localMeanIntensity(int channel, int kernelWidth, int kernelHeight, int j, int i){
		int widthOffset = (int)Math.floor(kernelWidth / 2.0f);
		int heightOffset = (int)Math.floor(kernelHeight / 2.0f);
		
		int widthUpperEdge = kernelWidth - widthOffset;
		int heightUpperEdge = kernelHeight - heightOffset;
		
		float localMean = 0.0f;
		float[] localHist = localHistogram(channel, kernelWidth, kernelHeight, j, i);
		
		// Loop through defined block size and calculate local mean
		Pixel p = new Pixel();
		int JL = 0;
		int IK = 0;
		int r = 0;
		for(int k = -heightOffset; k < heightUpperEdge; ++k){
			for(int l = -widthOffset; l < widthUpperEdge; ++l){
				JL = j + l;
				IK = i + k;
				
				if(checkPixelLocation(JL, IK)){
					p.setPixel(originalImage.getRGB(JL, IK));
					r = p.getChannel(channel);
					localMean += r * localHist[r];
				}
			}
		}
		
		return localMean;
	}
	
	public float localIntensityVariance(int channel, int kernelWidth, int kernelHeight, int j, int i){
		int widthOffset = (int)Math.floor(kernelWidth / 2.0f);
		int heightOffset = (int)Math.floor(kernelHeight / 2.0f);
		
		int widthUpperEdge = kernelWidth - widthOffset;
		int heightUpperEdge = kernelHeight - heightOffset;
		
		float localVariance = 0.0f;
		float localMean = localMeanIntensity(channel, kernelWidth, kernelHeight, j, i);
		float[] localHist = localHistogram(channel, kernelWidth, kernelHeight, j, i);
		
		// Loop through defined block size and calculate local variance
		Pixel p = new Pixel();
		int JL = 0;
		int IK = 0;
		int r = 0;
		for(int k = -heightOffset; k < heightUpperEdge; ++k){
			for(int l = -widthOffset; l < widthUpperEdge; ++l){
				JL = j + l;
				IK = i + k;
				
				if(checkPixelLocation(JL, IK)){
					p.setPixel(originalImage.getRGB(JL, IK));
					r = p.getChannel(channel);
					localVariance += (r - localMean)*(r - localMean)*localHist[r];
				}
			}
		}
		
		return localVariance;
	}
	
	// Returns linear function defined by 2 points
	public Function<Integer, Float> getLinearFunction(int x0, int y0, int x1, int y1){
		int dy = y1-y0;
		int dx = x1-x0;
		
		// Handle edge case when two points lie on the same vertical line
		if(dx == 0)
			return x -> (float)y0;
		
		return x -> ((float)dy/dx)*((float)x-x0)+(float)y0;
	}
	
	// Returns table for piecewise linear function based on list of coordinates
	// Coords format is x0, y0, x1, y1, ...
	// Assumption is that x0 <= x1 <= x2 <= ...
	public HashMap<Integer, Function<Integer, Float>> getPiecewiseLinearTable(Integer... coords){
		// Create table of functions
		HashMap<Integer, Function<Integer, Float>> functions = new HashMap<>();
		// Add first function
		functions.put(0, getLinearFunction(coords[0], coords[1], coords[2], coords[3]));
		
		// If there are more than 2 points then add additional functions to the table
		if(coords.length > 4){
			int index = coords.length - 2;
			
			for(int i = 2; i < index; i+=2)
				functions.put(i, getLinearFunction(coords[i], coords[i+1], coords[i+2], coords[i+3]));
		}
		
		return functions;
	}
	
	// When giving values, it is assumed that y values are in range [0-maxValue] and x values are in range [0-255]
	// Important: This histogram is approximately normalized meaning its sum is approximately 1.0
	public float[] getCustomHistogramPiecewiseLinear(int maxValue, Integer... coords){
		float[] hist = new float[256];
		
		HashMap<Integer, Function<Integer, Float>> functions = getPiecewiseLinearTable(coords);
		
		// Find histogram values for every number in range [0-255]
		int intervalBorderIndex = -1;
		for(int i = 0; i < 256; ++i){
			intervalBorderIndex = -1;
			
			for(int k = 0; k < coords.length; k+=2){
				// If specific interval is found
				if(intervalBorderIndex != -1) break;
				
				if(i <= coords[k])
					intervalBorderIndex = k-2;
			}
			
			hist[i] = functions.get(intervalBorderIndex < 0 ? 0 : intervalBorderIndex).apply(i);
		}
		
		// Normalize potential histogram values (we still don't know if area under it will be 1 under normalization)
		// Also calculate discrete sum so that we can adjust histogram so that its sum is approximately 1
		float discreteSum = 0.0f;
		for(int i = 0; i < 256; ++i){
			hist[i] /= maxValue;
			discreteSum += hist[i];
		}
		
		// Discrete area is just the sum of histogram values, and we want to make sure it is approximately 1
		for(int i = 0; i < 256; ++i)
			hist[i] /= discreteSum;
		
		return hist;
	}
	
	public String getFileExtension(File fileName){
		String[] temp = fileName.toString().split("\\.");
		return temp[temp.length - 1];
	}
	
	public boolean checkPixelLocation(int x, int y){
		if(x < 0 || x >= width) return false;
		if(y < 0 || y >= height) return false;
		return true;
	}
	
	public Pixel getMaxIntensities(){
		Pixel p = new Pixel();
		Pixel max = new Pixel();
		for(int i = 0; i < height; ++i){
			for(int j = 0; j < width; ++j){
				p.setPixel(originalImage.getRGB(j, i));
				
				if(p.getChannel(RED_CHANNEL) > max.getChannel(RED_CHANNEL))
					max.setChannel(RED_CHANNEL, p.getChannel(RED_CHANNEL));
				if(p.getChannel(GREEN_CHANNEL) > max.getChannel(GREEN_CHANNEL))
					max.setChannel(GREEN_CHANNEL, p.getChannel(GREEN_CHANNEL));
				if(p.getChannel(BLUE_CHANNEL) > max.getChannel(BLUE_CHANNEL))
					max.setChannel(BLUE_CHANNEL, p.getChannel(BLUE_CHANNEL));
			}
		}
		
		return max;
	}
	
	public Pixel[] getMinMaxPixel(Pixel[][] pixels){
		Pixel min = new Pixel();
		Pixel max = new Pixel();
		
		Pixel[] minMax = {min, max};
		
		for(int i = 0; i < pixels.length; ++i){
			for(int j = 0; j < pixels[0].length; ++j){
				if(pixels[i][j] == null) continue;
				
				if(pixels[i][j].r < min.r) min.r = pixels[i][j].r;
				if(pixels[i][j].g < min.g) min.g = pixels[i][j].g;
				if(pixels[i][j].b < min.b) min.b = pixels[i][j].b;
				
				if(pixels[i][j].r > max.r) max.r = pixels[i][j].r;
				if(pixels[i][j].g > max.g) max.g = pixels[i][j].g;
				if(pixels[i][j].b > max.b) max.b = pixels[i][j].b;
			}
		}
		
		minMax[0] = min;
		minMax[1] = max;
		
		return minMax;
	}
	
	public void interpolateToStandardInterval(Pixel[][] pixels, Pixel[] minMax){
		float sR = 1.0f / (minMax[1].r - minMax[0].r) * 255;
		float sG = 1.0f / (minMax[1].g - minMax[0].g) * 255;
		float sB = 1.0f / (minMax[1].b - minMax[0].b) * 255;
		
		for(int i = 0; i < pixels.length; ++i){
			for(int j = 0; j < pixels[0].length; ++j){
				if(pixels[i][j] == null) continue;
				
				pixels[i][j].subElementWise(minMax[0]);
				pixels[i][j].scale(sR, sG, sB);
			}
		}
	}
	
	public static void main(String[] args){
		// SOMETIMES THERE ARE NOTICEABLE ERRORS EVEN THOUGH THE PROCEDURES ARE RIGHT
		// THIS IS BECAUSE int IS USED IN PIXEL CLASS INSTEAD OF FLOAT
		
		DIP dip = new DIP();
		/*dip.loadImage(new File("images/xxx.tif"));
		dip.plotImageHistogram(new File("images/aaa_histR.png"), DIP.RED_CHANNEL, 40.0f);
		float[] customHist = dip.getCustomHistogramPiecewiseLinear(1000, 0, 0, 200, 200, 255, 1000);
		dip.grayscale();
		dip.shift();
		dip.histogramEqualization(DIP.RED_CHANNEL);
		dip.shift();
		dip.histogramEqualization(DIP.GREEN_CHANNEL);
		dip.shift();
		dip.histogramEqualization(DIP.BLUE_CHANNEL);
		dip.shift();
		
		dip.plotImageHistogram(new File("images/aaa_histR_after.png"), DIP.RED_CHANNEL, 40.0f);
		dip.saveOriginalImage("images/aaa");*/
		
		
		/*dip.loadImage(new File("images/pool.jpg"));
		dip.grayscale();
		dip.shift();
		dip.histogramEqualization(DIP.RED_CHANNEL);
		dip.shift();
		dip.histogramEqualization(DIP.GREEN_CHANNEL);
		dip.shift();
		dip.histogramEqualization(DIP.BLUE_CHANNEL);
		dip.shift();
		dip.saveOriginalImage("images/aaa");*/
		
		/*dip.loadImage(new File("images/dipxe.tif"));
		dip.highboostFilter(7, 1.0f);
		dip.saveSupportImage("images/XXXX_sharp");*/
		
		/*dip.loadImage(new File("images/moon.tif"));
		dip.laplacianBasic();
		dip.saveSupportImage("images/XXXX");*/
		
		dip.loadImage(new File("images/tesla.jpg"));
		dip.median(RED_CHANNEL, 9);
		dip.shift();
		dip.median(GREEN_CHANNEL, 9);
		dip.shift();
		dip.median(BLUE_CHANNEL, 9);
		dip.shift();
		dip.saveOriginalImage("images/XXXX");
		
	}
}