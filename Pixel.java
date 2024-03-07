import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.ArrayList;

class Pixel{
	public int a = 255, r = 0, g = 0, b = 0;
	public Pixel(){
		
	}
	public Pixel(int pixelInt){
		setPixel(pixelInt);
	}
	public Pixel(int r, int g, int b){
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	public Pixel(int a, int r, int g, int b){
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	public void setPixel(int pixelInt){
		a = (pixelInt >> 24) & 0xff;
		r = (pixelInt >> 16) & 0xff;
        g = (pixelInt >> 8) & 0xff;
        b = (pixelInt) & 0xff;
	}
	
	public int getChannel(int channel){
		if(channel == 0) return r;
		if(channel == 1) return g;
		if(channel == 2) return b;
		return -1;
	}
	
	public void setChannel(int channel, int value){
		if(channel == 0) r = value;
		if(channel == 1) g = value;
		if(channel == 2) b = value;
	}
	
	public static float normalize(int value, int max){
		return (float)value/max;
	}
	
	public static int denormalize(float value, float max){
		return (int)Math.round(value * max);
	}
	
	public static int clamp(int value, int min, int max){
		if(value < min) value = min;
		if(value > max) value = max;
		return value;
	}
	
	public static float clamp(float value, float min, float max){
		if(value < min) value = min;
		if(value > max) value = max;
		return value;
	}
	
	public void clampElementWise(int min, int max){
		r = clamp(r, min, max);
		g = clamp(g, min, max);
		b = clamp(b, min, max);
	}
	
	public void clampRed(int min, int max){
		r = clamp(r, min, max);
	}
	
	public void clampGreen(int min, int max){
		g = clamp(g, min, max);
	}
	
	public void clampBlue(int min, int max){
		b = clamp(b, min, max);
	}
	
	public static Pixel clampElementWise(int pixelValue, int min, int max){
		Pixel p = new Pixel(pixelValue);
		return new Pixel(clamp(p.r, min, max), clamp(p.g, min, max), clamp(p.b, min, max));
	}
	
	public static Pixel clampElementWise(Pixel p, int min, int max){
		return new Pixel(clamp(p.r, min, max), clamp(p.g, min, max), clamp(p.b, min, max));
	}
	
	public void setPixelElementWise(int channelValue){
		r = g = b = channelValue;
	}
	
	public void setPixelElementWise(float channelValue){
		r = g = b = Math.round(channelValue);
	}
	
	public void scale(float scale){
		r *= scale;
		g *= scale;
		b *= scale;
	}
	
	public void scale (float sR, float sG, float sB){
		r *= sR;
		g *= sG;
		b *= sB;
	}
	
	public void scaleElementWise(Pixel scalePixel){
		r *= scalePixel.r;
		g *= scalePixel.g;
		b *= scalePixel.b;
	}
	
	public static Pixel scale(int pixelInt, float scale){
		Pixel p = new Pixel(pixelInt);
		return new Pixel(p.a, Math.round(p.r * scale), Math.round(p.g * scale), Math.round(p.b * scale));
	}
	
	public static Pixel scale(Pixel p, float scale){
		return new Pixel(p.a, Math.round(p.r * scale), Math.round(p.g * scale), Math.round(p.b * scale));
	}
	
	public void addElementWise(int pixelInt){
		Pixel p = new Pixel(pixelInt);
		this.r += p.r;
		this.g += p.g;
		this.b += p.b;
	}
	
	public void addElementWise(Pixel p){
		this.r += p.r;
		this.g += p.g;
		this.b += p.b;
	}
	
	public void subElementWise(int pixelInt){
		Pixel p = new Pixel(pixelInt);
		this.r -= p.r;
		this.g -= p.g;
		this.b -= p.b;
	}
	
	public void subElementWise(Pixel p){
		this.r -= p.r;
		this.g -= p.g;
		this.b -= p.b;
	}
	
	public void mulElementWise(int pixelInt){
		Pixel p = new Pixel(pixelInt);
		this.r *= p.r;
		this.g *= p.g;
		this.b *= p.b;
	}
	
	public void mulElementWise(Pixel p){
		this.r *= p.r;
		this.g *= p.g;
		this.b *= p.b;
	}
	
	public static Pixel addElementWise(int pixelInt1, int pixelInt2){
		Pixel p1 = new Pixel(pixelInt1);
		Pixel p2 = new Pixel(pixelInt2);
		
		return new Pixel(255, p1.r + p2.r, p1.g + p2.g, p1.b + p2.b);
	}
	
	public static Pixel addElementWise(Pixel p1, Pixel p2){
		return new Pixel(255, p1.r + p2.r, p1.g + p2.g, p1.b + p2.b);
	}
	
	public static Pixel subElementWise(int pixelInt1, int pixelInt2){
		Pixel p1 = new Pixel(pixelInt1);
		Pixel p2 = new Pixel(pixelInt2);
		
		return new Pixel(255, p1.r - p2.r, p1.g - p2.g, p1.b - p2.b);
	}
	
	public static Pixel subElementWise(Pixel p1, Pixel p2){
		return new Pixel(255, p1.r - p2.r, p1.g - p2.g, p1.b - p2.b);
	}
	
	public static Pixel mulElementWise(int pixelInt1, int pixelInt2){
		Pixel p1 = new Pixel(pixelInt1);
		Pixel p2 = new Pixel(pixelInt2);
		
		return new Pixel(255, p1.r * p2.r, p1.g * p2.g, p1.b * p2.b);
	}
	
	public static Pixel mulElementWise(Pixel p1, Pixel p2){
		return new Pixel(255, p1.r * p2.r, p1.g * p2.g, p1.b * p2.b);
	}
	
	public int toInt(){
		int result = a;
		result <<= 8;
		result |= r;
		result <<= 8;
		result |= g;
		result <<= 8;
		result |= b;
		
		return result;
	}
	
	public int getAverageInt(){
		float result = 0.0f;
		result += r;
		result += g;
		result += b;
		result /= 3.0f;
		return Math.round(result);
	}
	
	public float getAverageFloat(){
		float result = 0.0f;
		result += r;
		result += g;
		result += b;
		result /= 3.0f;
		return result;
	}
	
	public Pixel toGrayscalePixel(){
		int g = getAverageInt();
		return new Pixel(1, g, g, g);
	}
	
	public int toGrayscaleInt(){
		int g = getAverageInt();
		return (new Pixel(1, g, g, g).toInt());
	}

	@Override
	public String toString(){
		return "[A:" + a + ", R:" + r + ", G:" + g + ", B:" + b + "]";
	}
}