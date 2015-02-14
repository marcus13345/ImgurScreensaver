import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.google.gson.Gson;

public class Main extends Canvas implements KeyListener, MouseMotionListener {
	public static void main(String[] args) {
		new ScrollingMain();
	}

	private JFrame frame;

	public Main() {

		frame = new JFrame();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);

		requestFocus();

		// Transparent 16 x 16 pixel cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		frame.getContentPane().setCursor(blankCursor);

		int pages = Integer.parseInt(new Variable("imgurscreensaver", "pages", "10", false).getValue());

		loopPages(pages, new Variable("imgurscreensaver", "subreddit", "annakendrick", false).getValue());

	}

	private void loopPages(int pages, String subreddit) {
		while (true)
			for (int i = 0; i < pages; i++)
				try {

					String path = "https://api.imgur.com/3/gallery/r/" + subreddit + "/time/" + i + ".json";

					HttpURLConnection connection = (HttpURLConnection) ((new URL(path)).openConnection());

					System.out.println("Connecting...");

					connection.setRequestMethod("GET");
					// TODO Auto-generated catch block
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

						for (Image image : response.data) {
							if (!(image.nsfw && filterNSFW) && !image.type.equals("image/gif")) {
								String url = "http://imgur.com/" + (image.id) + (parseExtension(image.type));
								currentimage = convertImage(new ImageIcon(new URL(url)).getImage());
								currentimage = getScaledImage(currentimage, getWidth(), getHeight());
								repaint();
								Thread.sleep(SLEEPTIME);
							}
						}

					}
				} catch (Exception e) {

				}
	}

	private BufferedImage convertImage(java.awt.Image image){
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

	private boolean filterNSFW = Boolean.parseBoolean(new Variable("imgurscreensaver", "filterNSFW", "true", false).getValue());
	private BufferedImage currentimage;
	private static final long SLEEPTIME = Long.parseLong(new Variable("imgurscreensaver", "SLEEPTIME", "1300", false).getValue());

	public void update(Graphics g) {
		java.awt.Image buffer = createImage(getWidth(), getHeight());
		Graphics g2 = buffer.getGraphics();
		paint(g2);
		g.drawImage(buffer, 0, 0, null);
	}

	private BufferedImage getScaledImage(BufferedImage image, int width, int height) throws IOException {

		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		double scaleY = (double) height / imageHeight;
		double scaleX = (double) width / imageWidth;

		double aspect = (double) imageWidth / imageHeight;
		double screenAspect = ((double)getWidth()/getHeight());
		System.out.println("" + aspect + "\n" + screenAspect);
		// fill or fit bit
		
		if (aspect < screenAspect || aspect > 2)
			if (scaleX > scaleY)
				scaleX = scaleY;
			else
				scaleY = scaleX;
		
		//this is fill. fill if aspect is between screen aspect and 2
		else
			if (scaleX < scaleY)
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
		BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffer.getGraphics();

		int newImageWidth = newImage.getWidth(null);
		int newImageHeight = newImage.getHeight(null);

		// do math, shove it on.
		g.drawImage(newImage, (width - newImageWidth) / 2, (height - newImageHeight) / 2, null);

		// return dat
		return buffer;
	}

	public void paint(Graphics g) {

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.WHITE);
		g.drawString("Connecting to server...", 0, getHeight());
		try {
			// BufferedImage image = (BufferedImage) ImageIO.read(new
			// URL(currentURL));

			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

			g.drawImage(currentimage, 0, 0, null);

		} catch (Exception e) {
			e.printStackTrace();
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