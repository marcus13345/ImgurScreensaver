import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.google.gson.Gson;

public class ScrollingMain extends Canvas implements KeyListener, MouseMotionListener {
	public static void main(String[] args) {
		new ScrollingMain();
	}

	private JFrame frame;

	private final Object LOCK = new Object();

	public ScrollingMain() {

		frame = new JFrame();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);

		createBufferStrategy(2);
		BufferStrategy bs = getBufferStrategy();
		
		requestFocus();

		// Transparent 16 x 16 pixel cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		frame.getContentPane().setCursor(blankCursor);

		final int pages = Integer.parseInt(new Variable("imgurscreensaver", "pages", "10", false).getValue());

		int subreddits = Integer.parseInt(new Variable("imgurscreensaver", "subreddits", "1", false).getValue());
		for(int i = 0; i < subreddits; i ++) {
			loopPages(pages, new Variable("imgurscreensaver", "subreddit-" + i, "annakendrick", false).getValue());
		}
		
		// loopPages(pages, "emmawatson");

		int elapsed = 0;
		while (true) {
			try {
				Thread.sleep(35 - elapsed);
			} catch (Exception e) {
				e.printStackTrace();
			}
			long start = System.currentTimeMillis();
			Graphics2D g = (Graphics2D)bs.getDrawGraphics();
			render(g);
			bs.show();
			long end = System.currentTimeMillis();
			elapsed = (int) (end - start);
		}

	}

	private ArrayList<Double> timeThings = new ArrayList<Double>();

	private void loopPages(final int pages, final String subreddit) {
		new Thread(new Runnable() {
			public void run() {
				ArrayList<BufferedImage> list = new ArrayList<BufferedImage>();
				synchronized (LOCK) {
					images.add(list);
					timeThings.add(0d);
				}

				while (true) {
					for (int page = 0; page < pages; page++) {
						try {

							String path = "https://api.imgur.com/3/gallery/r/" + subreddit + "/time/" + page + ".json";

							HttpURLConnection connection = (HttpURLConnection) ((new URL(path)).openConnection());

							System.out.println("Connecting...");

							connection.setRequestMethod("GET");

							connection.addRequestProperty("Authorization", "client-id 76535d44f1f94da");

							connection.connect();

							System.out.println("Response recieved with code " + connection.getResponseCode());

							if (connection.getResponseCode() == 200) {

								InputStream responseStream = connection.getInputStream();
								StringBuilder builder = new StringBuilder();
								int j = -1;
								while ((j = responseStream.read()) != -1)
									builder.append((char) j);

								System.out.println(builder.toString());

								Gson gson = new Gson();
								ImageArray response = gson.fromJson(builder.toString(), ImageArray.class);

								for (int imageCounter = 0; imageCounter < response.data.length;) {

									Image image = response.data[imageCounter];

									if (image.type.equals("image/gif"))
										imageCounter++;

									else if (!(image.nsfw && filterNSFW) && list.size() < (3*images.size())) {

										String url = "http://imgur.com/" + (image.id) + (parseExtension(image.type));
										BufferedImage toAdd = convertImage(new ImageIcon(new URL(url)).getImage());
										toAdd = getScaledImage(toAdd, getWidth(), getHeight() / images.size());

										synchronized (LOCK) {

											list.add(toAdd);

										}

										imageCounter++;
									} else {
										Thread.sleep(SLEEPTIME);
									}

								}

							}
						} catch (Exception e) {

						}
					}
				}

			}
		}).start();
	}

	private BufferedImage convertImage(java.awt.Image image) {
		BufferedImage buffer = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		buffer.getGraphics().drawImage(image, 0, 0, null);
		return buffer;
	}

	private String parseExtension(String type) {
		if (type.equals("image/jpeg")) {
			return ".jpg";
		} else if (type.equals("image/png")) {
			return ".png";
		} else if (type.equals("image/gif")) {
			return ".gif";
		} else {
			return ".jpg";
		}
	}

	private ArrayList<ArrayList<BufferedImage>> images = new ArrayList<ArrayList<BufferedImage>>();

	private boolean filterNSFW = Boolean.parseBoolean(new Variable("imgurscreensaver", "filterNSFW", "true", false).getValue());

	private static final long SLEEPTIME = 500;

	private BufferedImage getScaledImage(BufferedImage image, int width, int height) throws IOException {

		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		double scaleY = (double) height / imageHeight;
		double scaleX = (double) width / imageWidth;

		double aspect = (double) imageWidth / imageHeight;
		double screenAspect = ((double) getWidth() / getHeight());
		// fill or fit bit

		if (scaleX > scaleY)
			scaleX = scaleY;
		else
			scaleY = scaleX;

		// give us the transform object thing
		AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);

		// then make the scaling algorithm thing.
		AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

		// out new image that we need to crop onto the buffer with the right
		// dimensions.
		BufferedImage newImage = bilinearScaleOp.filter(image, new BufferedImage((int) (imageWidth * scaleX), (int) (imageHeight * scaleY), image.getType()));
		// Image newImage = image.getScaledInstance((int) (imageWidth * scaleX),
		// (int) (imageWidth * scaleY), Image.SCALE_SMOOTH);

		// make the buffer
		BufferedImage buffer = new BufferedImage((int) (imageWidth * scaleX), (int) (imageHeight * scaleY), BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffer.getGraphics();

		int newImageWidth = newImage.getWidth(null);
		int newImageHeight = newImage.getHeight(null);

		// do math, shove it on.
		g.drawImage(newImage, 0, 0, null);

		// return dat
		return buffer;
	}

	public void render(Graphics g) {

		synchronized (LOCK) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.WHITE);
			g.drawString("Connecting to server...", 0, getHeight());
			try {
				// BufferedImage image = (BufferedImage) ImageIO.read(new
				// URL(currentURL));
				if (images.size() > 0) {
					/*
					 * g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(),
					 * getHeight());
					 */
					int i = 0;

					for (ArrayList<BufferedImage> list : images) {
						int xPos = (int) (0 - timeThings.get(i));
						for (BufferedImage image : list) {
							g.drawImage(image, xPos, i * (getHeight() / images.size()), null);
							xPos += image.getWidth();
						}
						timeThings.set(i, timeThings.get(i) + 5d/images.size());
						int firstWidth = list.get(0).getWidth();
						if (timeThings.get(i) > firstWidth) {
							timeThings.set(i, timeThings.get(i) - firstWidth);
							list.remove(0);
						}

						i++;
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void close() {
		frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		close();
	}

	private boolean moved = false;

	@Override
	public void mouseMoved(MouseEvent e) {
		if (moved)
			close();
		else
			moved = true;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		close();
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		close();
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		close();
	}

}