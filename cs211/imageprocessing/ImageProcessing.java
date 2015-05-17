package cs211.imageprocessing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.video.Capture;

public class ImageProcessing extends PApplet {

	public float BRIGHTNESS = 160;
	public float SATURATION = 100;
	public float HUE = 50;
	
	public float INTENSITY = 100;
	
	public float SOBEL = 0.7f;
	
	private float max_area = 550000;
	private float min_area = 10000;
	public final int MIN_VOTES = 10;

	
	private PImage img;
	
	@Override
	public void setup() {
		img = loadImage("board1.jpg");
		size(640 * 3, 480);
		img.resize(640, 480);
	}

	@Override
	public void draw() {
		background(color(150, 150, 10));
		
		PImage sobel = sobel(intensity_threshold(blur(color_threshold(img))));
		image(img, 0, 0);
		displayQuads(hough(sobel, 4));
		image(sobel, 640 * 2, 0);
	}
	
	public List<PVector> displayQuads(List<PVector> lines){
		QuadGraph qgraph = new QuadGraph();
			qgraph.build(lines, img.width, img.height);
		
		ArrayList<PVector> foundQuad = new ArrayList<>();
		List<int[]> quads =  qgraph.findCycles();
		for (int[] quad : quads) {
			
			PVector l1 = lines.get(quad[0]);
			PVector l2 = lines.get(quad[1]);
			PVector l3 = lines.get(quad[2]);
			PVector l4 = lines.get(quad[3]);
			// (intersection() is a simplified version of the
			// intersections() method you wrote last week, that simply
			// return the coordinates of the intersection between 2 lines)
			PVector c12 = getIntersections(l1, l2);
			PVector c23 = getIntersections(l2, l3);
			PVector c34 = getIntersections(l3, l4);
			PVector c41 = getIntersections(l4, l1);
			
			/*
			 * Not needed for milestone
			if(
					QuadGraph.isConvex(c12,  c23, c34,  c41) &&
					 QuadGraph.nonFlatQuad(c12,  c23, c34,  c41) &&
					  QuadGraph.validArea(c12,  c23,  c34,  c41, max_area, min_area)
			){
				// Choose a random, semi-transparent colour
				Random random = new Random();
				fill(color(min(255, random.nextInt(300)),
				min(255, random.nextInt(300)),
				min(255, random.nextInt(300)), 50));
				
				quad(c12.x,c12.y,c23.x,c23.y,c34.x,c34.y,c41.x,c41.y);

				fill(color(255, 255, 255));
				text("1", c12.x, c12.y);
				text("2", c23.x, c23.y);
				text("3", c34.x, c34.y);
				text("4", c41.x, c41.y);
				foundQuad.add(c12);
				foundQuad.add(c23);
				foundQuad.add(c34);
				foundQuad.add(c41);
			}
			*/
		}
		return foundQuad;
	}

	private PVector getIntersections(PVector l1, PVector l2) {
		return getIntersections(Arrays.asList(new PVector[]{l1, l2})).get(0);
	}

	private ArrayList<PVector> getIntersections(List<PVector> lines) {
		ArrayList<PVector> intersections = new ArrayList<PVector>();
		for (int i = 0; i < lines.size() - 1; i++) {
			PVector l1 = lines.get(i);
			for (int j = i + 1; j < lines.size(); j++) {
				PVector l2 = lines.get(j);
				float d = cos(l2.y) * sin(l1.y) - cos(l1.y) * sin(l2.y);
				// compute the intersection and add it to 'intersections'
				int x = (int) ((l2.x * sin(l1.y) - l1.x * sin(l2.y)) / d), y = (int) ((-l2.x
						* cos(l1.y) + l1.x * cos(l2.y)) / d);
				// draw the intersection
				fill(255, 128, 128);
				ellipse(x, y, 10, 10);
				intersections.add(new PVector(x, y, 1));
			}
		}
		return intersections;
	}

	public PImage blur(PImage img) {
		int[][] kernel = { 
				{ 9, 12, 9 }, 
				{ 12, 15, 12 }, 
				{ 9, 12, 9 } };
		
		PImage result = createImage(img.width, img.height, RGB);
		// clear the image
		for (int i = 0; i < img.width * img.height; i++) {
			result.pixels[i] = color(0);
		}

		float weight = 1f;
		for (int i = 1; i < img.width - 1; i++) {
			for (int j = 1; j < img.height - 1; j++) {

				float c = 0;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++) {
						int index = (j + y) * img.width + (i + x);

						c += green(img.pixels[index]) * kernel[y + 1][1 + x];
					}
				}
				c /= weight;
				// println((c));
				result.set(i, j, color(c));
			}
		}
		return result;
	}
	
	public final float GREEN = 90f;
	public PImage color_threshold(PImage image) {
		PImage result = createImage(image.width, image.height, RGB);
		int c;
		for (int i = 0; i < image.width * image.height; i++) {
			c =  saturation(image.pixels[i]) > SATURATION &&
					hue(image.pixels[i]) < GREEN + HUE &&
						hue(image.pixels[i]) > GREEN - HUE &&
					brightness(image.pixels[i]) < BRIGHTNESS
					? image.pixels[i] : color(0, 0, 0);

			result.pixels[i] = c;
		}
		return result;
	}
	public PImage intensity_threshold(PImage image) {
		PImage result = createImage(image.width, image.height, RGB);
		int c;
		for (int i = 0; i < image.width * image.height; i++) {
			c =  brightness(image.pixels[i]) > INTENSITY ? image.pixels[i] : color(0, 0, 0);

			result.pixels[i] = c;
		}
		return result;
	}
	

	public List<PVector> hough(PImage edgeImg, int nLines) {
		float discretizationStepsPhi = 0.025f;
		float discretizationStepsR = 2.5f;
		// dimensions of the accumulator
		int phiDim = (int) (Math.PI / discretizationStepsPhi);
		int rDim = (int) (((edgeImg.width + edgeImg.height) * 2 + 1) / discretizationStepsR);

		// pre-compute the sin and cos values
		float[] tabSin = new float[phiDim];
		float[] tabCos = new float[phiDim];
		float ang = 0;
		float inverseR = 1.f / discretizationStepsR;
		for (int accPhi = 0; accPhi < phiDim; ang += discretizationStepsPhi, accPhi++) {
			// we can also pre-multiply by (1/discretizationStepsR) since we
			// need it in the Hough loop
			tabSin[accPhi] = (float) (Math.sin(ang) * inverseR);
			tabCos[accPhi] = (float) (Math.cos(ang) * inverseR);
		}

		// our accumulator (with a 1 pix margin around)
		int[] accumulator = new int[(phiDim + 2) * (rDim + 2)];
		for (int y = 0; y < edgeImg.height; y++) {
			for (int x = 0; x < edgeImg.width; x++) {
				if (brightness(edgeImg.pixels[y * edgeImg.width + x]) != 0) {

					for (float phi = 0; phi < PI; phi += discretizationStepsPhi) {

						int accPhi = Math.round(phi / discretizationStepsPhi);
						float r = (x * cos(phi) + y * sin(phi)) / discretizationStepsR;
						int accR = (int) (r + ((rDim - 1) * 0.5f));
						accumulator[(accPhi + 1) * (rDim + 2) + accR + 1]++;
					}
				}
			}
		}
		/*
		 * Display accumulator
		 */
		PImage houghImg = createImage(rDim + 2, phiDim + 2, ALPHA);
		for(int i = 0;i < accumulator.length;i++){
			houghImg.pixels[i] = color(min(255, accumulator[i]));
		}
		houghImg.updatePixels();
		houghImg.resize(img.width, img.height);
		/*
		 * 
		 */

		List<Integer> bestCandidates = new ArrayList<>();

		// size of the region we search for a local maximum
		int neighbourhood = 89;

		for (int accR = 0; accR < rDim; accR++) {
			for (int accPhi = 0; accPhi < phiDim; accPhi++) {
				// compute current index in the accumulator
				int idx = (accPhi + 1) * (rDim + 2) + accR + 1;
				if (accumulator[idx] > MIN_VOTES) {
					boolean bestCandidate = true;
					// iterate over the neighbourhood
					for (int dPhi = -neighbourhood / 2; dPhi < neighbourhood / 2 + 1; dPhi++) {
						// check we are not outside the image
						if (accPhi + dPhi < 0 || accPhi + dPhi >= phiDim)
							continue;
						for (int dR = -neighbourhood / 2; dR < neighbourhood / 2 + 1; dR++) {
							// check we are not outside the image
							if (accR + dR < 0 || accR + dR >= rDim)
								continue;
							int neighbourIdx = (accPhi + dPhi + 1) * (rDim + 2)
									+ accR + dR + 1;
							if (accumulator[idx] < accumulator[neighbourIdx]) {
								// the current idx is not a local maximum!
								bestCandidate = false;
								break;
							}
						}
						if (!bestCandidate)
							break;
					}
					if (bestCandidate) {
						// the current idx *is* a local maximum
						bestCandidates.add(idx);
					}
				}
			}
		}

		final int[] buffer = accumulator;
		Collections.sort(bestCandidates, new Comparator<Integer>() {
			@Override
			public int compare(Integer i1, Integer i2) {
				if (buffer[i1] > buffer[i2]
						|| (accumulator[i1] == accumulator[i2] && i1 < i2)) {
					return -1;
				} else
					return 1;
			}

		});
		if (nLines <= bestCandidates.size())
			bestCandidates = bestCandidates.subList(0, nLines);

		List<PVector> lines = new ArrayList<PVector>();
		int idx, c = 0;
		int[] colors = { color(255, 0, 0), color(0, 255, 0), color(0, 0, 255),
				color(255, 255, 0), color(255, 0, 255), color(0, 255, 255) };
		for (Integer i : bestCandidates) {
			idx = i;
			int accPhi = idx / (rDim + 2) - 1;
			int accR = idx - (accPhi + 1) * (rDim + 2) - 1;
			float r = (accR - (rDim - 1) * 0.5f) * discretizationStepsR;
			float phi = accPhi * discretizationStepsPhi;
			stroke(colors[c++ % colors.length]);
			drawLine(phi, r);
			lines.add(new PVector(r, phi));
		}
		image(houghImg, 640, 0);

		return lines;
	}

	private void drawLine(float phi, float r) {

		int x0 = 0;
		int y0 = (int) (r / sin(phi));
		int x1 = (int) (r / cos(phi));
		int y1 = 0;
		int x2 = width;
		int y2 = (int) (-cos(phi) / sin(phi) * x2 + r / sin(phi));
		int y3 = height;
		int x3 = (int) (-(y3 - r / sin(phi)) * (sin(phi) / cos(phi)));
		// Finally, plot the lines
		if (y0 > 0) {
			if (x1 > 0)
				line(x0, y0, x1, y1);
			else if (y2 > 0)
				line(x0, y0, x2, y2);
			else
				line(x0, y0, x3, y3);
		} else {
			if (x1 > 0) {
				if (y2 > 0)
					line(x1, y1, x2, y2);
				else
					line(x1, y1, x3, y3);
			} else
				line(x2, y2, x3, y3);
		}
	}

	public PImage sobel(PImage img) {
		float[][] hKernel = { { 0, 1, 0 }, { 0, 0, 0 }, { 0, -1, 0 } };
		float[][] vKernel = { { 0, 0, 0 }, { 1, 0, -1 }, { 0, 0, 0 } };
		PImage result = createImage(img.width, img.height, ALPHA);
		// clear the image
		for (int i = 0; i < img.width * img.height; i++) {
			result.pixels[i] = color(0);
		}
		float max = 0;
		float[] buffer = new float[img.width * img.height];
		// *************************************
		// Implement here the double convolution
		// *************************************
		for (int y = 2; y < img.height - 2; y++) { // Skip top and bottom edges
			for (int x = 2; x < img.width - 2; x++) { // Skip left and right

				float c1 = 0, c2 = 0;
				for (int xx = 0; xx < 3; xx++) {
					for (int yy = 0; yy < 3; yy++) {
						c1 += brightness(img.get(xx + x - 1, yy + y - 1))
								* hKernel[xx][yy];
					}
				}

				for (int xx = 0; xx < 3; xx++) {
					for (int yy = 0; yy < 3; yy++) {
						c2 += brightness(img.get(xx + x - 1, yy + y - 1))
								* vKernel[xx][yy];
					}
				}
				float sum = sqrt(pow(c1, 2) + pow(c2, 2));
				if (sum > max)
					max = sum;

				buffer[x + y * result.width] = sum;
			}
		}

		for (int y = 2; y < img.height - 2; y++) { // Skip top and bottom edges
			for (int x = 2; x < img.width - 2; x++) { // Skip left and right
				if (buffer[y * img.width + x] > (int) (max * SOBEL)) { // 30% of
																		// the
																		// max
					result.pixels[y * img.width + x] = color(255);
				} else {
					result.pixels[y * img.width + x] = color(0);
				}
			}
		}
		return result;
	}
}
