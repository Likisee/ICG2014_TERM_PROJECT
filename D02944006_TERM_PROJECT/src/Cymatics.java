import java.awt.*;
import java.applet.Applet;
import java.util.Vector;
import java.util.Random;
import java.awt.image.*;
import java.lang.Math;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

class CymaticsCanvas extends Canvas {
	CymaticsFrame cf;

	CymaticsCanvas(CymaticsFrame f) {
		cf = f;
	}

	public Dimension getPreferredSize() {
		return new Dimension(300, 400);
	}

	public void update(Graphics g) {
		cf.updateCymatics(g);
	}

	public void paint(Graphics g) {
		cf.updateCymatics(g);
	}
};

class CymaticsLayout implements LayoutManager {
	public CymaticsLayout() {
	}

	public void addLayoutComponent(String name, Component c) {
	}

	public void removeLayoutComponent(Component c) {
	}

	public Dimension preferredLayoutSize(Container target) {
		return new Dimension(500, 500);
	}

	public Dimension minimumLayoutSize(Container target) {
		return new Dimension(100, 100);
	}

	public void layoutContainer(Container target) {
		Insets insets = target.insets();
		
		// targetw, targeth -> getComponent(0)
		int targetw = target.size().width - (insets.left + insets.right);
		int cw = targetw * 7 / 10;
		if (target.getComponentCount() == 1) {
			cw = targetw;
		}
		int targeth = target.size().height - (insets.top + insets.bottom);
		target.getComponent(0).move(insets.left, insets.top);
		target.getComponent(0).resize(cw, targeth);
		
		// barwidth -> the rest of the getComponent(>0)
		int barwidth = targetw - cw; // (ratio: 3 / 10)
		cw = cw + insets.left;
		int i;
		int h = insets.top;
		for (i = 1; i < target.getComponentCount(); i++) { // For each Component
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				if (m instanceof Scrollbar) {
					d.width = barwidth;
				}
				if (m instanceof Choice && d.width > barwidth) {
					d.width = barwidth;
				}
				if (m instanceof Label) {
					h += d.height / 5;
					d.width = barwidth;
				}
				m.move(cw, h);
				m.resize(d.width, d.height);
				h = h + d.height;
			}
		}
	}
};

public class Cymatics extends Applet implements ComponentListener {

	boolean started = false;
	
	static CymaticsFrame cf;
	
	void destroyFrame() {
		if (cf != null) {
			cf.dispose();
		}
		cf = null;
		repaint();
	}

	void showFrame() {
		if (cf == null) {
			started = true;
			cf = new CymaticsFrame(this);
			cf.init();
			repaint();
		}
	}

	public void paint(Graphics g) {
		String s = "Applet is open in a separate window.";
		if (!started) {
			s = "Applet is starting.";
		} else if (cf == null) {
			s = "Applet is finished.";
		} else if (cf.useFrame) {
			cf.triggerShow();
		}
		g.drawString(s, 10, 30);
	}

	public void init() {
		addComponentListener(this);
	}
	
	public static void main(String args[]) {
		cf = new CymaticsFrame(null);
		cf.init();
	}
	
	/**************************************************************************
	 * ComponentEvent
	 *************************************************************************/

	public void componentHidden(ComponentEvent e) {}

	public void componentMoved(ComponentEvent e) {}

	public void componentShown(ComponentEvent e) {
		showFrame();
	}

	public void componentResized(ComponentEvent e) {
	}

	public void destroy() {
		if (cf != null) {
			cf.dispose();
		}
		cf = null;
		repaint();
	}
};

class CymaticsFrame extends Frame implements ComponentListener, ActionListener, AdjustmentListener, MouseMotionListener, MouseListener, ItemListener {
	
	CymaticsCanvas cv;
	
	Cymatics applet;

	CymaticsFrame(Cymatics c) {
		super("CYMATICS Music Player by CSu (D02944006@ntu.edu.tw)");
		applet = c;
		useFrame = true;
		showControls = true;
		adjustResolution = true;
	}
	
	/**************************************************************************
	 * Variable Initial
	 *************************************************************************/	

	Dimension windowSize;
	Image dbimage;

	Random random;
	int gridSizeX;
	int gridSizeY;
	int gridSizeXY;
	int gw;
	int windowWidth = 50;
	int windowHeight = 50;
	int windowOffsetX = 0;
	int windowOffsetY = 0;
	int windowBottom = 0;
	int windowRight = 0;
	
	Container main;

	Choice musicChooser;
	Choice setupChooser;
	Choice sourceChooser;
	Choice colorChooser;
	
	Button clearWaveButton;
	Button clearBorderButton;
	Button setBorderButton;
	Button view3dButton;
	Button stopButton;
		
	Scrollbar speedBar;
	Scrollbar resBar;
	Scrollbar freqBar;
	Scrollbar brightnessBar;
	double brightMult = 1;
	Scrollbar auxBar;
	Label auxLabel;
	int auxFunction;
	
	static final double pi = 3.14159265358979323846;
	public static final int sourceRadius = 5;
	public static final double freqMult = .0233333;
	
	static final int SWF_SIN = 0;
	
	static final int AUX_NONE = 0;
	static final int AUX_SPEED = 1;
	
	static final int SRC_NONE = 0;
	static final int SRC_1S = 1;
	static final int SRC_2S = 2;
	static final int SRC_4S = 3;
	static final int SRC_1S_MOVING = 4;
	static final int SRC_1S_PLANE = 5;
	static final int SRC_2S_PLANE = 6;
	
	static final int MODE_SETFUNC = 0;
	static final int MODE_WALLS = 1;

	OscillationSource sources[];
	float func[];
	float funci[];
	boolean walls[];
	boolean exception[];
	int pixels[];
	
	Color wallColor, posColor, negColor, zeroColor, sourceColor;
	Color schemeColors[][];

	long startTime;
	Method timerMethod;
	int timerDiv;
	
	MemoryImageSource imageSource;
	public boolean useFrame;
	boolean showControls;
	boolean useBufferedImage = false;
	
	Vector setupList;
	Setup setup;

	boolean adjustResolution = true;
	boolean increaseResolution = false;
	
	boolean sourcePlane = false;
	boolean sourceMoving = false;
	double movingSourcePos = 0;
	int sourceCount = -1;
	int selectedSource = -1;
	int sourceFreqCount = -1;
	int sourceWaveform = SWF_SIN;
	int sourceIndex;
	
	boolean dragging;
	boolean dragClear;
	boolean dragSet;
	int dragX, dragY, dragStartX = -1, dragStartY;

	int freqBarValue;
	double freqTimeZero;
	double timeT;

	boolean is3dView = false;
	boolean isStop = false;
	double dampcoef = 1;
	
	/**************************************************************************
	 * Applet Main
	 *************************************************************************/	
	
	public void init() {
		// useFrame? showControls? main!!
		try {
			if (applet != null) {
				// useFrame: Default true
				String param = applet.getParameter("useFrame");
				if (param != null && param.equalsIgnoreCase("false")) {
					useFrame = false;
				}
				// showControls: Default false
				param = applet.getParameter("showControls");
				if (param != null && param.equalsIgnoreCase("false")) {
					showControls = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (useFrame) {
			main = this;
		} else {
			main = applet;
		}
		
		// useBufferedImage
		String jv = System.getProperty("java.class.version");
		double jvf = new Double(jv).doubleValue();
		if (jvf >= 48) { // Java 1.4
			useBufferedImage = true;
		}

		// timerMethod & timerDiv (nano vs milli)
		try {
			Class sysclass = Class.forName("java.lang.System");
			timerMethod = sysclass.getMethod("nanoTime", null);
			timerDiv = 1000000;
			if (timerMethod == null) {
				timerMethod = sysclass.getMethod("currentTimeMillis", null);
				timerDiv = 1;
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		
		main.setLayout(new CymaticsLayout());
		cv = new CymaticsCanvas(this);
		cv.addComponentListener(this);
		cv.addMouseMotionListener(this);
		cv.addMouseListener(this);
		main.add(cv);

		// Initial setupList -> setupChooser
		setupList = new Vector();
		Setup s = new SingleSourceSetup(); // Default SingleSourceSetup
		while (s != null) {
			setupList.addElement(s);
			s = s.createNext();
		}
		setupChooser = new Choice();
		int i;
		for (i = 0; i != setupList.size(); i++) {
			setupChooser.add("預設主題: " + ((Setup) setupList.elementAt(i)).getName());
		}
		setupChooser.addItemListener(this);
		if (showControls) {
			main.add(setupChooser);
		}

		// Initial OscillationSource & sourceChooser
		sources = new OscillationSource[20];
		sourceChooser = new Choice();
		sourceChooser.add("無");			// 0
		sourceChooser.add("1點波源");	// 1
		sourceChooser.add("2點波源");	// 2
		sourceChooser.add("4點波源");	// 3
		sourceChooser.add("移動波源");	// 4
		sourceChooser.add("1線波源");	// 5
		sourceChooser.add("2線波源");	// 6
		sourceChooser.select(SRC_1S);
		sourceChooser.addItemListener(this);
		if (showControls) {
//			main.add(sourceChooser);
		}

		// Initial colorChooser
		colorChooser = new Choice();
		colorChooser.addItemListener(this);
		if (showControls) {
			main.add(colorChooser);
		}

		// Initial clearWaveButton
		clearWaveButton = new Button("清空水波");
		clearWaveButton.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		if (showControls) {
			main.add(clearWaveButton);
		}
		clearWaveButton.addActionListener(this);
		
		// Initial clearBorderButton
		clearBorderButton = new Button("清空邊界");
		clearBorderButton.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		if (showControls) {
			main.add(clearBorderButton);
		}
		clearBorderButton.addActionListener(this);
		
		// Initial setBorderButton
		setBorderButton = new Button("新增邊界");
		setBorderButton.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		if (showControls) {
			main.add(setBorderButton);
		}
		setBorderButton.addActionListener(this);
		
		// Initial view3DButton
		view3dButton = new Button("切3D視角");
		view3dButton.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		view3dButton.setPreferredSize(new Dimension(178, 50));
		if (showControls) {
			main.add(view3dButton);
		}
		view3dButton.addActionListener(this);
		
		// Initial stopButton
		stopButton = new Button("暫停");
		stopButton.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		stopButton.setPreferredSize(new Dimension(178, 50));
		if (showControls) {
			main.add(stopButton);
		}
		stopButton.addActionListener(this);
		
		// Initial speedBar
		Label l = new Label("模擬速度", Label.CENTER);
		l.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		speedBar = new Scrollbar(Scrollbar.HORIZONTAL, 8, 1, 1, 100); // Default: 8/100
		speedBar.addAdjustmentListener(this);
		if (showControls) {
//			main.add(l);
//			main.add(speedBar);
		}

		// Initial resBar
		l = new Label("解析度", Label.CENTER);
		l.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		resBar = new Scrollbar(Scrollbar.HORIZONTAL, 110, 5, 5, 400); // Default: 110/400
		resBar.addAdjustmentListener(this);
		if (showControls) {
//			main.add(l);
//			main.add(resBar);
		}
		setResolution();

		// Initial freqBar
		l = new Label("水波頻率", Label.CENTER);
		l.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		freqBar = new Scrollbar(Scrollbar.HORIZONTAL, freqBarValue = 15, 1, 1, 30); // Default: 15/30
		freqBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(freqBar);
		}

		// Initial brightnessBar
		l = new Label("對比色差", Label.CENTER);
		l.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		brightnessBar = new Scrollbar(Scrollbar.HORIZONTAL, 27, 1, 1, 1200); // Default: 27/1200
		brightnessBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(brightnessBar);
		}

		// Initial auxBar
		auxLabel = new Label("", Label.CENTER);
		auxBar = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, 30); // Default: 1/30
		auxBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(auxLabel);
			main.add(auxBar);
		}

		schemeColors = new Color[20][8];

		try {
			String param;
			
			param = applet.getParameter("setup");
			if (param != null) {
				setupChooser.select(Integer.parseInt(param));
			}
			
			param = applet.getParameter("setupClass");
			if (param != null) {
				for (i = 0; i != setupList.size(); i++) {
					if (setupList.elementAt(i).getClass().getName().equalsIgnoreCase("CymaticsFrame$" + param)) {
						setupChooser.select(i);
						break;
					}
				}
			}
			
		} catch (Exception e) {
			if (applet != null) {
				e.printStackTrace();
			}
		}
		if (colorChooser.getItemCount() == 0) {
			addDefaultColorScheme();
		}
		doColor();
		setup = (Setup) setupList.elementAt(setupChooser.getSelectedIndex());
		reinit();
		cv.setBackground(Color.black);
		cv.setForeground(Color.lightGray);
		startTime = getTimeMillis();

		if (useFrame) {
			resize(800, 640);
			handleResize();
			Dimension x = getSize();
			Dimension screen = getToolkit().getScreenSize();
			setLocation((screen.width - x.width) / 2, (screen.height - x.height) / 2);
			show();
		} else {
			hide();
			handleResize();
			applet.validate();
		}
		main.requestFocus();
	}

	void reinit() {
		reinit(true);
	}

	void reinit(boolean setup) {
		sourceCount = -1;
		System.out.print("reinit " + gridSizeX + " " + gridSizeY + "\n");
		gridSizeXY = gridSizeX * gridSizeY;
		gw = gridSizeY;
		func = new float[gridSizeXY];
		funci = new float[gridSizeXY];
		walls = new boolean[gridSizeXY];
		exception = new boolean[gridSizeXY];
		int i, j;
		if (setup) {
			doSetup();
		}

	}

	boolean shown = false;

	public void triggerShow() {
		if (!shown)
			show();
		shown = true;
	}

	void handleResize() {
		Dimension d = windowSize = cv.getSize();
		if (windowSize.width == 0) {
			return;
		}
		pixels = null;
		if (useBufferedImage) {
			try {
				Class biclass = Class.forName("java.awt.image.BufferedImage");
				Class dbiclass = Class.forName("java.awt.image.DataBufferInt");
				Class rasclass = Class.forName("java.awt.image.Raster");
				Constructor cstr = biclass.getConstructor(new Class[] { int.class, int.class, int.class });
				dbimage = (Image) cstr.newInstance(new Object[] { new Integer(d.width), new Integer(d.height), new Integer(BufferedImage.TYPE_INT_RGB) });
				Method m = biclass.getMethod("getRaster", null);
				Object ras = m.invoke(dbimage, null);
				Object db = rasclass.getMethod("getDataBuffer", null).invoke(ras, null);
				pixels = (int[]) dbiclass.getMethod("getData", null).invoke(db, null);
			} catch (Exception ee) {
				System.out.println("BufferedImage failed");
			}
		}
		if (pixels == null) {
			pixels = new int[d.width * d.height];
			int i;
			for (i = 0; i != d.width * d.height; i++)
				pixels[i] = 0xFF000000;
			imageSource = new MemoryImageSource(d.width, d.height, pixels, 0, d.width);
			imageSource.setAnimated(true);
			imageSource.setFullBufferUpdates(true);
			dbimage = cv.createImage(imageSource);
		}
	}

	public boolean handleEvent(Event ev) {
		if (ev.id == Event.WINDOW_DESTROY) {
			destroyFrame();
			return true;
		}
		return super.handleEvent(ev);
	}
	
	void destroyFrame() {
		if (applet == null) {
			dispose();
		} else {
			applet.destroyFrame();
		}
	}

	int frames = 0;
	int steps = 0;

	boolean moveRight = true;
	boolean moveDown = true;

	public void updateCymatics(Graphics realg) {
		if (windowSize == null || windowSize.width == 0) {
			// this is the workaround for some weird bug in IE which causes the applet to not show up properly
			handleResize();
			return;
		}
		
		long sysTime = getTimeMillis();
		
		if (increaseResolution) {
			increaseResolution = false;
			if (resBar.getValue() < 495) {
				setResolution(resBar.getValue() + 10);
			}
		}
		
		double tadd = 0;
		if (!isStop) {
			int val = 5; // speedBar.getValue();
			tadd = val * .05;
		}

		boolean stopFunc = dragging && selectedSource == -1 && is3dView == false;
		if (isStop) {
			stopFunc = true;
		}
		
		int iterCount = speedBar.getValue();
		if (!stopFunc) {
			int iter;
			int mxx = gridSizeX - 1;
			int mxy = gridSizeY - 1;
			for (iter = 0; iter != iterCount; iter++) {
				int jstart, jend, jinc;
				if (moveDown) {
					// we process the rows in alternate directions each time to avoid any directional bias.
					jstart = 1;
					jend = mxy;
					jinc = 1;
					moveDown = false;
				} else {
					jstart = mxy - 1;
					jend = 0;
					jinc = -1;
					moveDown = true;
				}
				moveRight = moveDown;
				float sinhalfth = 0;
				float sinth = 0;
				float scaleo = 0;
				for (int j = jstart; j != jend; j += jinc) {
					int istart, iend, iinc;
					if (moveRight) {
						iinc = 1;
						istart = 1;
						iend = mxx;
						moveRight = false;
					} else {
						iinc = -1;
						istart = mxx - 1;
						iend = 0;
						moveRight = true;
					}
					int gi = j * gw + istart;
					int giEnd = j * gw + iend;
					for (; gi != giEnd; gi += iinc) {
						// calculate equilibrium point of this element's oscillation
						float previ = func[gi - 1];
						float nexti = func[gi + 1];
						float prevj = func[gi - gw];
						float nextj = func[gi + gw];
						float basis = (nexti + previ + nextj + prevj) * .25f;
						if (exception[gi]) {

							sinhalfth = (float) Math.sin(tadd / 2);
							sinth = (float) (Math.sin(tadd) * dampcoef);
							scaleo = (float) (1 - Math.sqrt(4 * sinhalfth * sinhalfth - sinth * sinth));
							
							if (walls[gi]) {
								continue;
							}

							if (walls[gi - 1])	previ = 0;
							if (walls[gi + 1])	nexti = 0;
							if (walls[gi - gw])	prevj = 0;
							if (walls[gi + gw])	nextj = 0;
							basis = (nexti + previ + nextj + prevj) * .25f;
						}
						// what we are doing here (aside from damping) is rotating the point (func[gi], funci[gi]) an angle tadd about the point (basis, 0). Rather than call atan2/sin/cos, we use this faster method using some precomputed info.
						float a = 0;
						float b = 0;
						a = func[gi] - basis;
						b = funci[gi];
						func[gi] = basis + a * scaleo - b * sinth;
						funci[gi] = b * scaleo + a * sinth;
					}
				}
				timeT += tadd;
				if (sourceCount > 0) {
					double w = freqBar.getValue() * (timeT - freqTimeZero) * freqMult;
					double w2 = w;
					double v = 0;
					double v2 = 0;
					v = Math.cos(w);
					if (sourceCount >= (sourcePlane ? 4 : 2)) {
						v2 = Math.cos(w2);
					} else if (sourceFreqCount == 2) {
						v = (v + Math.cos(w2)) * .5;
					}
					for (int j = 0; j != sourceCount; j++) {
						if ((j % 2) == 0) {
							sources[j].v = (float) (v * setup.sourceStrength());
						} else{
							sources[j].v = (float) (v2 * setup.sourceStrength());
						}
					}
					if (sourcePlane) {
						for (int j = 0; j != sourceCount / 2; j++) {
							OscillationSource src1 = sources[j * 2];
							OscillationSource src2 = sources[j * 2 + 1];
							OscillationSource src3 = sources[j];
							drawPlaneSource(src1.x, src1.y, src2.x, src2.y, src3.v, w);
						}
					} else {
						if (sourceMoving) {
							int sy;
							movingSourcePos += tadd * .02 * auxBar.getValue();
							double wm = movingSourcePos;
							int h = windowHeight - 3;
							wm %= h * 2;
							sy = (int) wm;
							if (sy > h) {
								sy = 2 * h - sy;
							}
							sy += windowOffsetY + 1;
							sources[0].y = sy;
						}
						for (int i = 0; i != sourceCount; i++) {
							OscillationSource src = sources[i];
							func[src.x + gw * src.y] = src.v;
							funci[src.x + gw * src.y] = 0;
						}
					}
				}
				setup.eachFrame();
				steps++;
			}
		}

		brightMult = Math.exp(brightnessBar.getValue() / 100. - 5.);
		if (is3dView) {
			draw3dView();
		} else {
			draw2dView();
		}

		if (imageSource != null) {
			imageSource.newPixels();
		}

		realg.drawImage(dbimage, 0, 0, this);
		if (dragStartX >= 0 && !is3dView) {
			int x = dragStartX * windowWidth / windowSize.width;
			int y = windowHeight - 1 - (dragStartY * windowHeight / windowSize.height);
			String s = "(" + x + "," + y + ")";
			realg.setColor(Color.white);
			FontMetrics fm = realg.getFontMetrics();
			int h = 5 + fm.getAscent();
			realg.fillRect(0, windowSize.height - h, fm.stringWidth(s) + 10, h);
			realg.setColor(Color.black);
			realg.drawString(s, 5, windowSize.height - 5);
		}

		if (!isStop) {
			long diff = getTimeMillis() - sysTime;
			// we want the time it takes for a wave to travel across the screen to be more-or-less constant, but don't do anything after 5 seconds
			if (adjustResolution && diff > 0 && sysTime < startTime + 1000 && windowOffsetX * diff / iterCount < 55) {
				increaseResolution = true;
				startTime = sysTime;
			}
			cv.repaint(0);
		}
	}

	void drawPlaneSource(int x1, int y1, int x2, int y2, float v, double w) {
		// correct x1, x2
		if (y1 == y2) {
			if (x1 == windowOffsetX) {
				x1 = 0;
			}
			if (x2 == windowOffsetX) {
				x2 = 0;
			}
			if (x1 == windowOffsetX + windowWidth - 1) {
				x1 = gridSizeX - 1;
			}
			if (x2 == windowOffsetX + windowWidth - 1) {
				x2 = gridSizeX - 1;
			}
		}
		
		// correct y1, y2
		if (x1 == x2) {
			if (y1 == windowOffsetY) {
				y1 = 0;
			}
			if (y2 == windowOffsetY) {
				y2 = 0;
			}
			if (y1 == windowOffsetY + windowHeight - 1) {
				y1 = gridSizeY - 1;
			}
			if (y2 == windowOffsetY + windowHeight - 1) {
				y2 = gridSizeY - 1;
			}
		}

		// need to draw a line from x1,y1 to x2,y2 smoothly by interpolation
		if (x1 == x2 && y1 == y2) {					// a straight line
			func[x1 + gw * y1] = v;
			funci[x1 + gw * y1] = 0;
		} else if (abs(y2 - y1) > abs(x2 - x1)) {	// y difference is greater, so we step along y's from min to max y and calculate x for each step
			double sgn = sign(y2 - y1);
			int x, y;
			for (y = y1; y != y2 + sgn; y += sgn) {
				x = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
				double ph = sgn * (y - y1) / (y2 - y1);
				int gi = x + gw * y;
				func[gi] = setup.calcSourcePhase(ph, v, w);
				funci[gi] = 0;
			}
		} else {									// x difference is greater, so we step along x's from min to max x and calculate y for each step
			double sgn = sign(x2 - x1);
			int x, y;
			for (x = x1; x != x2 + sgn; x += sgn) {
				y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
				double ph = sgn * (x - x1) / (x2 - x1);
				int gi = x + gw * y;
				func[gi] = setup.calcSourcePhase(ph, v, w);
				funci[gi] = 0;
			}
		}
	}

	/**************************************************************************
	 * Draw 2D
	 *************************************************************************/
	
	void draw2dView() {
		int ix = 0;
		int i, j, k, l;
		for (j = 0; j != windowHeight; j++) {
			ix = windowSize.width * (j * windowSize.height / windowHeight);
			int j2 = j + windowOffsetY;
			int gi = j2 * gw + windowOffsetX;
			int y = j * windowSize.height / windowHeight;
			int y2 = (j + 1) * windowSize.height / windowHeight;
			for (i = 0; i != windowWidth; i++, gi++) {
				int x = i * windowSize.width / windowWidth;
				int x2 = (i + 1) * windowSize.width / windowWidth;
				int i2 = i + windowOffsetX;
				double dy = func[gi] * brightMult;
				if (dy < -1)
					dy = -1;
				if (dy > 1)
					dy = 1;
				int col = 0;
				int colR = 0, colG = 0, colB = 0;
				if (walls[gi]) {
					colR = wallColor.getRed();
					colG = wallColor.getGreen();
					colB = wallColor.getBlue();
				} 
				else if (dy < 0) {
					double d1 = -dy;
					double d2 = 1 - d1;
					colR = (int) (negColor.getRed() * d1 + zeroColor.getRed() * d2);
					colG = (int) (negColor.getGreen() * d1 + zeroColor.getGreen() * d2);
					colB = (int) (negColor.getBlue() * d1 + zeroColor.getBlue() * d2);
				} else {
					double d1 = dy;
					double d2 = 1 - dy;
					colR = (int) (posColor.getRed() * d1 + zeroColor.getRed() * d2);
					colG = (int) (posColor.getGreen() * d1 + zeroColor.getGreen() * d2);
					colB = (int) (posColor.getBlue() * d1 + zeroColor.getBlue() * d2);
				}
				col = (255 << 24) | (colR << 16) | (colG << 8) | (colB);
				for (k = 0; k != x2 - x; k++, ix++) {
					for (l = 0; l != y2 - y; l++) {
						pixels[ix + l * windowSize.width] = col;
					}
				}
			}
		}
		for (i = 0; i != sourceCount; i++) {
			OscillationSource src = sources[i];
			int xx = src.getScreenX();
			int yy = src.getScreenY();
			plotSource(i, xx, yy);
		}
	}
	
	// draw a circle the slow and dirty way
	void plotSource(int n, int xx, int yy) {
		int rad = sourceRadius;
		int j;
		int col = (sourceColor.getRed() << 16) | (sourceColor.getGreen() << 8) | (sourceColor.getBlue()) | 0xFF000000;
		if (n == selectedSource) {
			col ^= 0xFFFFFF;
		}
		for (j = 0; j <= rad; j++) {
			int k = (int) (Math.sqrt(rad * rad - j * j) + .5);
			plotPixel(xx + j, yy + k, col);
			plotPixel(xx + k, yy + j, col);
			plotPixel(xx + j, yy - k, col);
			plotPixel(xx - k, yy + j, col);
			plotPixel(xx - j, yy + k, col);
			plotPixel(xx + k, yy - j, col);
			plotPixel(xx - j, yy - k, col);
			plotPixel(xx - k, yy - j, col);
			plotPixel(xx, yy + j, col);
			plotPixel(xx, yy - j, col);
			plotPixel(xx + j, yy, col);
			plotPixel(xx - j, yy, col);
		}
	}
	
	void plotPixel(int x, int y, int pix) {
		if (x < 0 || x >= windowSize.width) {
			return;
		}
		try {
			pixels[x + y * windowSize.width] = pix;
		} catch (Exception e) {
		}
	}
	
	/**************************************************************************
	 * Draw 3D
	 *************************************************************************/
	
	double realxmx, realxmy, realymz, realzmy, realzmx, realymadd, realzmadd;
	double viewAngle = pi, viewAngleDragStart;
	double viewZoom = .775, viewZoomDragStart;
	double viewAngleCos = -1, viewAngleSin = 0;
	double viewHeight = -38, viewHeightDragStart;
	double scalex, scaley;
	int centerX3d, centerY3d;
	int xpoints[] = new int[4], ypoints[] = new int[4];
	final double viewDistance = 66;

	void draw3dView() {
		int half = gridSizeX / 2;
		scaleworld();
		int x, y;
		int xdir, xstart, xend;
		int ydir, ystart, yend;
		int sc = windowRight - 1;

		// figure out what order to render the grid elements so that the ones in front are rendered first.
		if (viewAngleCos > 0) {
			ystart = sc;
			yend = windowOffsetY - 1;
			ydir = -1;
		} else {
			ystart = windowOffsetY;
			yend = sc + 1;
			ydir = 1;
		}
		if (viewAngleSin < 0) {
			xstart = windowOffsetX;
			xend = sc + 1;
			xdir = 1;
		} else {
			xstart = sc;
			xend = windowOffsetX - 1;
			xdir = -1;
		}
		boolean xFirst = (viewAngleSin * xdir < viewAngleCos * ydir);

		for (x = 0; x != windowSize.width * windowSize.height; x++) {
			pixels[x] = 0xFF000000;
		}

		double zval = .1;
		double zval2 = zval * zval;

		for (x = xstart; x != xend; x += xdir) {
			for (y = ystart; y != yend; y += ydir) {
				if (!xFirst) {
					x = xstart;
				}
				for (; x != xend; x += xdir) {
					int gi = x + gw * y;
					map3d(x - half, y - half, func[gi], xpoints, ypoints, 0);
					map3d(x + 1 - half, y - half, func[gi + 1], xpoints, ypoints, 1);
					map3d(x - half, y + 1 - half, func[gi + gw], xpoints, ypoints, 2);
					map3d(x + 1 - half, y + 1 - half, func[gi + gw + 1], xpoints, ypoints, 3);
					double qx = func[gi + 1] - func[gi];
					double qy = func[gi + gw] - func[gi];
					// calculate lighting
					double normdot = (qx + qy + zval) * (1 / 1.73) / Math.sqrt(qx * qx + qy * qy + zval2);
					int col = computeColor(gi, normdot);
					fillTriangle(xpoints[0], ypoints[0], xpoints[1], ypoints[1], xpoints[3], ypoints[3], col);
					fillTriangle(xpoints[0], ypoints[0], xpoints[2], ypoints[2], xpoints[3], ypoints[3], col);
					if (xFirst) {
						break;
					}
				}
			}
			if (!xFirst)
				break;
		}
	}
	
	double scaleMult;
	void scaleworld() {
		scalex = viewZoom * (windowSize.width / 4) * viewDistance / 8;
		scaley = -scalex;
		int y = (int) (scaley * viewHeight / viewDistance);

		centerX3d = windowSize.width / 2;
		centerY3d = windowSize.height / 2 - y;
		scaleMult = 16. / (windowWidth / 2);
		realxmx = -viewAngleCos * scaleMult * scalex;
		realxmy = viewAngleSin * scaleMult * scalex;
		realymz = -brightMult * scaley;
		realzmy = viewAngleCos * scaleMult;
		realzmx = viewAngleSin * scaleMult;
		realymadd = -viewHeight * scaley;
		realzmadd = viewDistance;
	}

	void map3d(double x, double y, double z, int xpoints[], int ypoints[], int pt) {
		double realx = realxmx * x + realxmy * y;
		double realy = realymz * z + realymadd;
		double realz = realzmx * x + realzmy * y + realzmadd;
		xpoints[pt] = centerX3d + (int) (realx / realz);
		ypoints[pt] = centerY3d - (int) (realy / realz);
	}
	
	int computeColor(int gix, double c) {
		double h = func[gix] * brightMult;
		if (c < 0)	c = 0;
		if (c > 1)	c = 1;
		c = .5 + c * .5;
		double redness = (h < 0) ? -h : 0;
		double grnness = (h > 0) ? h : 0;
		if (redness > 1)	redness = 1;
		if (grnness > 1)	grnness = 1;
		if (grnness < 0)	grnness = 0;
		if (redness < 0)	redness = 0;
		double grayness = (1 - (redness + grnness)) * c;
		double gray = .6;
		int bi = (int) ((gray * grayness) * 255);
		return 0xFF000000 | bi;
	}

	void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int col) {
		if (x1 > x2) {
			if (x2 > x3) {
				// x1 > x2 > x3
				int ay = interp(x1, y1, x3, y3, x2);
				fillTriangle1(x3, y3, x2, y2, ay, col);
				fillTriangle1(x1, y1, x2, y2, ay, col);
			} else if (x1 > x3) {
				// x1 > x3 > x2
				int ay = interp(x1, y1, x2, y2, x3);
				fillTriangle1(x2, y2, x3, y3, ay, col);
				fillTriangle1(x1, y1, x3, y3, ay, col);
			} else {
				// x3 > x1 > x2
				int ay = interp(x3, y3, x2, y2, x1);
				fillTriangle1(x2, y2, x1, y1, ay, col);
				fillTriangle1(x3, y3, x1, y1, ay, col);
			}
		} else {
			if (x1 > x3) {
				// x2 > x1 > x3
				int ay = interp(x2, y2, x3, y3, x1);
				fillTriangle1(x3, y3, x1, y1, ay, col);
				fillTriangle1(x2, y2, x1, y1, ay, col);
			} else if (x2 > x3) {
				// x2 > x3 > x1
				int ay = interp(x2, y2, x1, y1, x3);
				fillTriangle1(x1, y1, x3, y3, ay, col);
				fillTriangle1(x2, y2, x3, y3, ay, col);
			} else {
				// x3 > x2 > x1
				int ay = interp(x3, y3, x1, y1, x2);
				fillTriangle1(x1, y1, x2, y2, ay, col);
				fillTriangle1(x3, y3, x2, y2, ay, col);
			}
		}
	}

	void fillTriangle1(int x1, int y1, int x2, int y2, int y3, int col) {
		// x2 == x3
		int dir = (x1 > x2) ? -1 : 1;
		int x = x1;
		if (x < 0) {
			x = 0;
			if (x2 < 0) {
				return;
			}
		}
		if (x >= windowSize.width) {
			x = windowSize.width - 1;
			if (x2 >= windowSize.width) {
				return;
			}
		}
		if (y2 > y3) {
			int q = y2;
			y2 = y3;
			y3 = q;
		}
		// y2 < y3
		while (x != x2 + dir) {
			int ya = interp(x1, y1, x2, y2, x);
			int yb = interp(x1, y1, x2, y3, x);
			if (ya < 0) {
				ya = 0;
			}
			if (yb >= windowSize.height) {
				yb = windowSize.height - 1;
			}

			for (; ya <= yb; ya++) {
				pixels[x + ya * windowSize.width] = col;
			}
			x += dir;
			if (x < 0 || x >= windowSize.width) {
				return;
			}
		}
	}
	
	int interp(int x1, int y1, int x2, int y2, int x) {
		if (x1 == x2) {
			return y1;
		}
		if (x < x1 && x < x2 || x > x1 && x > x2) {
			System.out.print("interp out of bounds\n");
		}
		return (int) (y1 + ((double) x - x1) * (y2 - y1) / (x2 - x1));
	}
	
	// TODO
	/**************************************************************************
	 * ColorScheme Setup
	 *************************************************************************/
	
	void addDefaultColorScheme() {

		decodeColorScheme(0, "#000000 #3333D6 #0000A3 #0000CC #CC0000", "Water");
		decodeColorScheme(1, "#800000 #ffffff #ffffff #808080 #CC0000", "Cymatics1: White Plate");
		decodeColorScheme(2, "#800000 #000000 #000000 #808080 #CC0000", "Cymatics2: Black Plate");
	}

	void decodeColorScheme(int cn, String s, String name) {
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			int i;
			for (i = 0; i != 5; i++)
				schemeColors[cn][i] = Color.decode(st.nextToken());
		}
		colorChooser.add(name);
	}
	
	/**************************************************************************
	 * WaveSource Setup
	 *************************************************************************/

	abstract class Setup {
		abstract String getName();

		abstract void select();

		void doSetupSources() {
			setSources();
		}

		void deselect() {
		}

		double sourceStrength() {
			return 1;
		}

		abstract Setup createNext();

		void eachFrame() {
		}

		float calcSourcePhase(double ph, float v, double w) {
			return v;
		}
	};

	class SingleSourceSetup extends Setup {
		String getName() {
			return "1點波源";
		}

		void select() {
//			setFreqBar(15);
		}

		Setup createNext() {
			return new DoubleSourceSetup();
		}
	}

	class DoubleSourceSetup extends Setup {
		String getName() {
			return "2點波源";
		}

		void select() {
//			setFreqBar(15);
		}

		void doSetupSources() {
			sourceChooser.select(SRC_2S);
			setSources();
			sources[0].y = gridSizeY / 2 - 8;
			sources[1].y = gridSizeY / 2 + 8;
			sources[0].x = sources[1].x = gridSizeX / 2;
		}

		Setup createNext() {
			return new QuadrupleSourceSetup();
		}
	}

	class QuadrupleSourceSetup extends Setup {
		String getName() {
			return "4點波源";
		}

		void select() {
//			setFreqBar(15);
		}
		
		void doSetupSources() {
			sourceChooser.select(SRC_4S);
			setSources();
		}

		Setup createNext() {
			return new PlaneWaveSetup();
		}
	}

	class PlaneWaveSetup extends Setup {
		String getName() {
			return "1線波源";
		}

		void select() {
//			setFreqBar(15);
		}
		
		void doSetupSources() {
			sourceChooser.select(SRC_1S_PLANE);
			setSources();
		}

		Setup createNext() {
			return new IntersectingPlaneWavesSetup();
		}
	}

	class IntersectingPlaneWavesSetup extends Setup {
		String getName() {
			return "交錯線波源";
		}

		void select() {
//			setFreqBar(15);
		}

		void doSetupSources() {
			sourceChooser.select(SRC_2S_PLANE);
			setSources();
			sources[0].y = sources[1].y = windowOffsetY;
			sources[0].x = windowOffsetX + 1;
			sources[2].x = sources[3].x = windowOffsetX;
			sources[2].y = windowOffsetY + 1;
			sources[3].y = windowOffsetY + windowHeight - 1;
		}

		Setup createNext() {
			return new DopplerSetup();
		}
	}

	class DopplerSetup extends Setup {
		String getName() {
			return "都普勒效應";
		}

		void select() {
//			setFreqBar(15);
		}

		void doSetupSources() {
			sourceChooser.select(SRC_1S_MOVING);
			setSources();
		}
		
		Setup createNext() {
			return null;
		}
	}
	
	/**************************************************************************
	 * Event
	 *************************************************************************/

	
	/**************************************************************************
	 * ComponentEvent
	 *************************************************************************/
	
	public void componentHidden(ComponentEvent e) {}

	public void componentMoved(ComponentEvent e) {}

	public void componentShown(ComponentEvent e) {
		cv.repaint();
	}

	public void componentResized(ComponentEvent e) {
		handleResize();
		cv.repaint(100);
	}
	
	/**************************************************************************
	 * ItemEvent
	 *************************************************************************/

	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == sourceChooser) {
			if (sourceChooser.getSelectedIndex() != sourceIndex) {
				setSources();
			}
		}
		if (e.getItemSelectable() == setupChooser) {
			doSetup();
		}
		if (e.getItemSelectable() == colorChooser) {
			doColor();
		}
	}

	void setSources() {
		int oldSCount = sourceCount;
		boolean oldPlane = sourcePlane;
		
		sourceIndex = sourceChooser.getSelectedIndex();
		sourcePlane = (sourceChooser.getSelectedIndex() >= SRC_1S_PLANE && sourceChooser.getSelectedIndex() <= SRC_2S_PLANE );
		
		sourceCount = 1;
		sourceFreqCount = 1;
		sourceMoving = false;
		sourceWaveform = SWF_SIN;
		
		
		switch (sourceChooser.getSelectedIndex()) {
		case 0:
			sourceCount = 0;
			break;
		case 2:
			sourceCount = 2;
			break;
		case 3:
			sourceCount = 4;
			break;
		case 4:
			sourceMoving = true;
			break;
		case 6:
			sourceCount = 2;
			break;
		}
		
		if (sourceMoving) {
			auxFunction = AUX_SPEED;
			auxBar.setValue(7);
			auxLabel.setText("移動速度");
			auxLabel.setFont(new Font( "Arial" , Font.BOLD , 40 ));
		}  else {
			auxFunction = AUX_NONE;
			auxBar.hide();
			auxLabel.hide();
		}
		if (auxFunction != AUX_NONE) {
			auxBar.show();
			auxLabel.show();
		}
		validate();

		if (sourcePlane) {
			sourceCount *= 2;
			if (!(oldPlane && oldSCount == sourceCount)) {
				int x2 = windowOffsetX + windowWidth - 1;
				int y2 = windowOffsetY + windowHeight - 1;
				sources[0] = new OscillationSource(windowOffsetX, windowOffsetY + 1);
				sources[1] = new OscillationSource(x2, windowOffsetY + 1);
				sources[2] = new OscillationSource(windowOffsetX, y2);
				sources[3] = new OscillationSource(x2, y2);
			}
		} else if (!(!oldPlane && oldSCount == sourceCount)) {
			sources[0] = new OscillationSource(gridSizeX / 2, windowOffsetY + 1);
			sources[1] = new OscillationSource(gridSizeX / 2, gridSizeY - windowOffsetY - 2);
			sources[2] = new OscillationSource(windowOffsetX + 1, gridSizeY / 2);
			sources[3] = new OscillationSource(gridSizeX - windowOffsetX - 2, gridSizeY / 2);
			for (int i = 4; i < sourceCount; i++) {
				sources[i] = new OscillationSource(windowOffsetX + 1 + i * 2, gridSizeY / 2);
			}
		}
	}
	
	void doSetup() {
		timeT = 0;
		if (resBar.getValue() < 32) {
			setResolution(32);
		}
		doClearWave();
		doClearBorder();
		// don't use previous source positions, use defaults
		sourceCount = -1;
		sourceChooser.select(SRC_1S);
		setFreqBar(5);
		setBrightness(15);
		auxBar.setValue(1);
		setup = (Setup) setupList.elementAt(setupChooser.getSelectedIndex());
		setup.select();
		setup.doSetupSources();
		calculateExceptions();
	}

	void setBrightness(int x) {
		double m = x / 5.;
		m = (Math.log(m) + 5.) * 100;
		brightnessBar.setValue((int) m);
	}
	
	void doColor() {
		int cn = colorChooser.getSelectedIndex();
		wallColor = schemeColors[cn][0];
		posColor = schemeColors[cn][1];
		negColor = schemeColors[cn][2];
		zeroColor = schemeColors[cn][3];
		sourceColor = schemeColors[cn][4];
	}
	
	/**************************************************************************
	 * ActionEvent
	 *************************************************************************/
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == clearWaveButton) {
			doClearWave();
			cv.repaint();
		}
		if (e.getSource() == clearBorderButton) {
			doClearBorder();
			cv.repaint();
		}
		if (e.getSource() == setBorderButton) {
			doSetBorder();
			cv.repaint();
		}
		if (e.getSource() == view3dButton) {
			do3dView();
			cv.repaint();
		}
		if (e.getSource() == stopButton) {
			doStop();
			cv.repaint();
		}
	}
	
	void doClearWave() {
		int x, y;
		for (x = 0; x != gridSizeXY; x++) {
			func[x] = funci[x] = 1e-10f;
		}
	}

	void doClearBorder() {
		int x, y;
		for (x = 0; x != gridSizeXY; x++) {
			walls[x] = false;
		}
		calculateExceptions();
	}

	void doSetBorder() {
		int x, y;
		for (x = 0; x < gridSizeX; x++) {
			setWall(x, windowOffsetY);
			setWall(x, windowBottom);
		}
		for (y = 0; y < gridSizeY; y++) {
			setWall(windowOffsetX, y);
			setWall(windowRight, y);
		}
		calculateExceptions();
	}
	
	void do3dView() {
		is3dView = !is3dView;
		if(is3dView) {
			view3dButton.setLabel("切2D視角");
		} else {
			view3dButton.setLabel("切3D視角");
		}
	}
	
	void doStop() {
		isStop = !isStop;
		if(isStop) {
			stopButton.setLabel("開始");
		} else {
			stopButton.setLabel("暫停");
		}
	}
	
	/**************************************************************************
	 * AdjustmentEvent
	 *************************************************************************/
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		System.out.print(((Scrollbar) e.getSource()).getValue() + "\n");
		if (e.getSource() == brightnessBar) {
			cv.repaint(0);
		}
		if (e.getSource() == freqBar) {
			setFreq();
		}
	}

	void setFreqBar(int x) {
		freqBar.setValue(x);
		freqBarValue = x;
		freqTimeZero = 0;
	}

	void setFreq() {
		// adjust time zero to maintain continuity in the freq func even though the frequency has changed.
		double oldfreq = freqBarValue * freqMult;
		freqBarValue = freqBar.getValue();
		double newfreq = freqBarValue * freqMult;
		freqTimeZero = timeT - oldfreq * (timeT - freqTimeZero) / newfreq; // newfreq * (timeT - freqTimeZero) = oldfreq * (timeT - freqTimeZero);
	}

	/**************************************************************************
	 * MouseEvent
	 *************************************************************************/

	void edit(MouseEvent e) {
		if (is3dView) {
			return;
		}
		int x = e.getX();
		int y = e.getY();
		if (selectedSource != -1) {
			x = x * windowWidth / windowSize.width;
			y = y * windowHeight / windowSize.height;
			if (x >= 0 && y >= 0 && x < windowWidth && y < windowHeight) {
				sources[selectedSource].x = x + windowOffsetX;
				sources[selectedSource].y = y + windowOffsetY;
			}
			return;
		}
		if (dragX == x && dragY == y)
			editFuncPoint(x, y);
		else {
			// need to draw a line from old x,y to new x,y and
			// call editFuncPoint for each point on that line. yuck.
			if (abs(y - dragY) > abs(x - dragX)) {
				// y difference is greater, so we step along y's from min to max y and calculate x for each step
				int x1 = (y < dragY) ? x : dragX;
				int y1 = (y < dragY) ? y : dragY;
				int x2 = (y > dragY) ? x : dragX;
				int y2 = (y > dragY) ? y : dragY;
				dragX = x;
				dragY = y;
				for (y = y1; y <= y2; y++) {
					x = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
					editFuncPoint(x, y);
				}
			} else {
				// x difference is greater, so we step along x's from min to max x and calculate y for each step
				int x1 = (x < dragX) ? x : dragX;
				int y1 = (x < dragX) ? y : dragY;
				int x2 = (x > dragX) ? x : dragX;
				int y2 = (x > dragX) ? y : dragY;
				dragX = x;
				dragY = y;
				for (x = x1; x <= x2; x++) {
					y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
					editFuncPoint(x, y);
				}
			}
		}
	}

	void editFuncPoint(int x, int y) {
		int xp = x * windowWidth / windowSize.width + windowOffsetX;
		int yp = y * windowHeight / windowSize.height + windowOffsetY;
		int gi = xp + yp * gw;
		if (!dragSet && !dragClear) {
			dragClear = func[gi] > .1;
			dragSet = !dragClear;
		}
		func[gi] = (dragSet) ? 1 : -1;
		funci[gi] = 0;
		cv.repaint(0);
	}

	void selectSource(MouseEvent me) {
		int x = me.getX();
		int y = me.getY();
		int i;
		for (i = 0; i != sourceCount; i++) {
			OscillationSource src = sources[i];
			int sx = src.getScreenX();
			int sy = src.getScreenY();
			int r2 = (sx - x) * (sx - x) + (sy - y) * (sy - y);
			if (sourceRadius * sourceRadius > r2) {
				selectedSource = i;
				return;
			}
		}
		selectedSource = -1;
	}
	
	public void mouseDragged(MouseEvent e) {
		if (is3dView) {
			view3dDrag(e);
		}
		if (!dragging) {
			selectSource(e);
		}
		dragging = true;
		edit(e);
		adjustResolution = false;
		cv.repaint(0);
	}

	public void mouseMoved(MouseEvent e) {
		if (dragging) {
			return;
		}
		int x = e.getX();
		int y = e.getY();
		dragStartX = dragX = x;
		dragStartY = dragY = y;
		viewAngleDragStart = viewAngle;
		viewHeightDragStart = viewHeight;
		selectSource(e);
		if (isStop) {
			cv.repaint(0);
		}
	}

	void view3dDrag(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		viewAngle = (dragStartX - x) / 40. + viewAngleDragStart;
		while (viewAngle < 0) {
			viewAngle += 2 * pi;
		}
		while (viewAngle >= 2 * pi) {
			viewAngle -= 2 * pi;
		}
		viewAngleCos = Math.cos(viewAngle);
		viewAngleSin = Math.sin(viewAngle);
		viewHeight = (dragStartY - y) / 10. + viewHeightDragStart;

		cv.repaint();
	}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {
		dragStartX = -1;
	}

	public void mousePressed(MouseEvent e) {
		adjustResolution = false;
		mouseMoved(e);
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
			return;
		}
		dragging = true;
		edit(e);
	}

	public void mouseReleased(MouseEvent e) {
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
			return;
		}
		dragging = false;
		dragSet = dragClear = false;
		cv.repaint();
	}
	
	/**************************************************************************
	 * Global Variable Setting Function
	 *************************************************************************/
	
	void setResolution(int x) {
		resBar.setValue(x);
		setResolution();
		reinit();
	}
	
	void setResolution() {
		windowWidth = windowHeight = resBar.getValue();
		int border = windowWidth / 9;
		if (border < 20) {
			border = 20;
		}
		windowOffsetX = windowOffsetY = border;
		gridSizeX = windowWidth + windowOffsetX * 2;
		gridSizeY = windowHeight + windowOffsetY * 2;
		windowBottom = windowOffsetY + windowHeight - 1;
		windowRight = windowOffsetX + windowWidth - 1;
	}
	
	void setWall(int x, int y) {
		walls[x + gw * y] = true;
	}
	
	/**************************************************************************
	 * Global Variable Calculating Function
	 *************************************************************************/
	
	void calculateExceptions() {
		int x, y;
		// if walls are in place on border, need to extend that through hidden area to avoid "leaks"
		for (x = 0; x != gridSizeX; x++) {
			for (y = 0; y < windowOffsetY; y++) {
				walls[x + gw * y] = walls[x + gw * windowOffsetY];
				walls[x + gw * (gridSizeY - y - 1)] = walls[x + gw * (gridSizeY - windowOffsetY - 1)];
			}
		}
		for (y = 0; y < gridSizeY; y++) {
			for (x = 0; x < windowOffsetX; x++) {
				walls[x + gw * y] = walls[windowOffsetX + gw * y];
				walls[gridSizeX - x - 1 + gw * y] = walls[gridSizeX - windowOffsetX - 1 + gw * y];
			}
		}
		// generate exceptional array, which is useful for doing special handling of elements
		for (x = 1; x < gridSizeX - 1; x++) {
			for (y = 1; y < gridSizeY - 1; y++) {
				int gi = x + gw * y;
				exception[gi] = walls[gi - 1] || walls[gi + 1] || walls[gi - gw] || walls[gi + gw] || walls[gi];
			}
		}
		// put some extra exceptions at the corners to ensure tadd2, sinth and etc to get calculated
		exception[1 + gw] = exception[gridSizeX - 2 + gw] = exception[1 + (gridSizeY - 2) * gw] = exception[gridSizeX - 2 + (gridSizeY - 2) * gw] = true;
	}

	
	/**************************************************************************
	 * Calculating Function
	 *************************************************************************/
	
	int abs(int x) {
		return x < 0 ? -x : x;
	}
	
	int sign(int x) {
		return (x < 0) ? -1 : (x == 0) ? 0 : 1;
	}
	
	long getTimeMillis() {
		try {
			Long time = (Long) timerMethod.invoke(null, new Object[] {});
			return time.longValue() / timerDiv;
		} catch (Exception ee) {
			ee.printStackTrace();
			return 0;
		}
	}
	
	/**************************************************************************
	 * Object Class
	 *************************************************************************/	
	
	class OscillationSource {
		int x;
		int y;
		float v;

		OscillationSource(int xx, int yy) {
			x = xx;
			y = yy;
		}

		int getScreenX() {
			return ((x - windowOffsetX) * windowSize.width + windowSize.width / 2) / windowWidth;
		}

		int getScreenY() {
			return ((y - windowOffsetY) * windowSize.height + windowSize.height / 2) / windowHeight;
		}
	};
}