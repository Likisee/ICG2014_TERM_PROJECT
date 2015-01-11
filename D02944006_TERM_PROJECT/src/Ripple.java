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

class RippleCanvas extends Canvas {
	RippleFrame pg;

	RippleCanvas(RippleFrame p) {
		pg = p;
	}

	public Dimension getPreferredSize() {
		return new Dimension(300, 400);
	}

	public void update(Graphics g) {
		pg.updateRipple(g);
	}

	public void paint(Graphics g) {
		pg.updateRipple(g);
	}
};

class RippleLayout implements LayoutManager {
	public RippleLayout() {
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
		for (i = 1; i < target.getComponentCount(); i++) { // TODO For each Component
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

public class Ripple extends Applet implements ComponentListener {
	static RippleFrame ogf;

	void destroyFrame() {
		if (ogf != null) {
			ogf.dispose();
		}
		ogf = null;
		repaint();
	}

	boolean started = false;

	public void init() {
		addComponentListener(this);
	}

	public static void main(String args[]) {
		ogf = new RippleFrame(null);
		ogf.init();
	}

	void showFrame() {
		if (ogf == null) {
			started = true;
			ogf = new RippleFrame(this);
			ogf.init();
			repaint();
		}
	}

	public void paint(Graphics g) {
		String s = "Applet is open in a separate window.";
		if (!started) {
			s = "Applet is starting.";
		} else if (ogf == null) {
			s = "Applet is finished.";
		} else if (ogf.useFrame) {
			ogf.triggerShow();
		}
		g.drawString(s, 10, 30);
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		showFrame();
	}

	public void componentResized(ComponentEvent e) {
	}

	public void destroy() {
		if (ogf != null) {
			ogf.dispose();
		}
		ogf = null;
		repaint();
	}
};

class RippleFrame extends Frame implements ComponentListener, ActionListener, AdjustmentListener, MouseMotionListener, MouseListener, ItemListener {

	public String getAppletInfo() {
		return "CYMATICS Player by CSu (D02944006@ntu.edu.tw)";
	}
	
	RippleCanvas cv;
	
	Ripple applet;

	RippleFrame(Ripple a) {
		super("CYMATICS Music Player");
		applet = a;
		useFrame = true;
		showControls = true;
		adjustResolution = true;
	}
	
	Thread engine = null;

	Dimension winSize;
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

	Choice setupChooser;
	Choice sourceChooser;
	Choice modeChooser;
	Choice colorChooser;
	
	Button blankButton;
	Button blankWallsButton;
	Button borderButton;
//	Button exportButton;
	
	Checkbox stoppedCheck;
	Checkbox fixedEndsCheck;
	Checkbox view3dCheck;
		
	Scrollbar speedBar;
	Scrollbar resBar;
	Scrollbar dampingBar;
	Scrollbar freqBar;
	Scrollbar brightnessBar;
	Scrollbar auxBar;
	
	static final double pi = 3.14159265358979323846;
	public static final int sourceRadius = 5;
	public static final double freqMult = .0233333;
	static final int mediumMax = 191;
	static final double mediumMaxIndex = .5;
	
	static final int SWF_SIN = 0;
	static final int SWF_SQUARE = 1;
	static final int SWF_PULSE = 2;
	
	static final int AUX_NONE = 0;
	static final int AUX_PHASE = 1;
	static final int AUX_FREQ = 2;
	static final int AUX_SPEED = 3;
	
//	static final int SRC_NONE = 0;
//	static final int SRC_1S1F = 1;
//	static final int SRC_2S1F = 3;
//	static final int SRC_2S2F = 4;
//	static final int SRC_4S1F = 6;
//	static final int SRC_1S1F_PULSE = 8;
//	static final int SRC_1S1F_MOVING = 9;
//	static final int SRC_1S1F_PLANE = 10;
//	static final int SRC_2S1F_PLANE = 12;
//	static final int SRC_1S1F_PLANE_PULSE = 14;
//	static final int SRC_1S1F_PLANE_PHASE = 15;
//	static final int SRC_6S1F = 16;
//	static final int SRC_8S1F = 17;
//	static final int SRC_10S1F = 18;
//	static final int SRC_12S1F = 19;
//	static final int SRC_16S1F = 20;
//	static final int SRC_20S1F = 21;
	static final int SRC_NONE = 0;
	static final int SRC_1S1F = 1;
	static final int SRC_2S1F = 2;
	static final int SRC_4S1F = 3;
	static final int SRC_1S1F_MOVING = 4;
	static final int SRC_1S1F_PLANE = 5;
	static final int SRC_2S1F_PLANE = 6;
	
	static final int MODE_SETFUNC = 0;
	static final int MODE_WALLS = 1;
	static final int MODE_MEDIUM = 2;
	static final int MODE_FUNCHOLD = 3;
	
	float func[];
	float funci[];
	OscSource sources[];
	boolean walls[];
	int medium[];
	float damp[];
	boolean exceptional[];
	
	public boolean useFrame;
	boolean showControls;
	boolean useBufferedImage = false;
	Method timerMethod;
	int timerDiv;
	
	Vector setupList;
	Setup setup;	
	

	
	
	Label auxLabel;
	double dampcoef;
	double freqTimeZero;
	double movingSourcePos = 0;
	double brightMult = 1;
	int dragX, dragY, dragStartX = -1, dragStartY;
	int selectedSource = -1;
	int sourceIndex;
	int freqBarValue;
	boolean dragging;
	boolean dragClear;
	boolean dragSet;
	double t;
	MemoryImageSource imageSource;
	int pixels[];
	int sourceCount = -1;
	boolean sourcePlane = false;
	boolean sourceMoving = false;
	boolean increaseResolution = false;
	boolean adjustResolution = true;
	int sourceFreqCount = -1;
	int sourceWaveform = SWF_SIN;
	int auxFunction;
	long startTime;
	Color wallColor, posColor, negColor, zeroColor, sourceColor;
	Color schemeColors[][];
	
//	ImportDialog impDialog;

//	int getrand(int x) {
//		int q = random.nextInt();
//		if (q < 0) {
//			q = -q;
//		}
//		return q % x;
//	}
	
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

//		int res = 110;
//		String os = System.getProperty("os.name");
		
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
		
		main.setLayout(new RippleLayout());
		cv = new RippleCanvas(this);
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
			setupChooser.add("Setup: " + ((Setup) setupList.elementAt(i)).getName());
		}
		setupChooser.addItemListener(this);
		if (showControls) {
			main.add(setupChooser);
		}

		// Initial OscSource & sourceChooser
//		add(new Label("Source mode:", Label.CENTER));
		sources = new OscSource[20];
		sourceChooser = new Choice();
//		sourceChooser.add("No Sources");	// 0 -> 0
//		sourceChooser.add("1 Src, 1 Freq");	// 1 -> 1
//		sourceChooser.add("1 Src, 2 Freq");	// 2
//		sourceChooser.add("2 Src, 1 Freq");	// 3 -> 2
//		sourceChooser.add("2 Src, 2 Freq");	// 4
//		sourceChooser.add("3 Src, 1 Freq");	// 5
//		sourceChooser.add("4 Src, 1 Freq");	// 6 -> 3
//		sourceChooser.add("1 Src, 1 Freq (Square)");
//		sourceChooser.add("1 Src, 1 Freq (Pulse)");
//		sourceChooser.add("1 Moving Src");	// 9 -> 4
//		sourceChooser.add("1 Plane Src, 1 Freq");	// 10 -> 5
//		sourceChooser.add("1 Plane Src, 2 Freq");	// 11
//		sourceChooser.add("2 Plane Src, 1 Freq");	// 12 -> 6
//		sourceChooser.add("2 Plane Src, 2 Freq");	// 13
//		sourceChooser.add("1 Plane 1 Freq (Pulse)");
//		sourceChooser.add("1 Plane 1 Freq w/Phase");
//		sourceChooser.add("6 Src, 1 Freq");
//		sourceChooser.add("8 Src, 1 Freq");
//		sourceChooser.add("10 Src, 1 Freq");
//		sourceChooser.add("12 Src, 1 Freq");
//		sourceChooser.add("16 Src, 1 Freq");
//		sourceChooser.add("20 Src, 1 Freq");
		sourceChooser.add("No Sources");	// 0
		sourceChooser.add("1 Src, 1 Freq");	// 1
		sourceChooser.add("2 Src, 1 Freq");	// 2
		sourceChooser.add("4 Src, 1 Freq");	// 3
		sourceChooser.add("1 Moving Src");	// 4
		sourceChooser.add("1 Plane Src, 1 Freq");	// 5
		sourceChooser.add("2 Plane Src, 1 Freq");	// 6
		sourceChooser.select(SRC_1S1F);
		sourceChooser.addItemListener(this);
		if (showControls) {
			main.add(sourceChooser);
		}

		// Initial modeChooser
//		add(new Label("Mouse mode:", Label.CENTER));
		modeChooser = new Choice();
		modeChooser.add("Mouse = Edit Wave");
		modeChooser.add("Mouse = Edit Walls");
		modeChooser.add("Mouse = Edit Medium");
		modeChooser.add("Mouse = Hold Wave");
		modeChooser.addItemListener(this);
		if (showControls) {
			main.add(modeChooser);
		} else {
			modeChooser.select(1);
		}

		// Initial colorChooser
		colorChooser = new Choice();
		colorChooser.addItemListener(this);
		if (showControls) {
			main.add(colorChooser);
		}

		// Initial blankButton
		blankButton = new Button("Clear Waves");
		if (showControls) {
			main.add(blankButton);
		}
		blankButton.addActionListener(this);
		
		// Initial blankWallsButton
		blankWallsButton = new Button("Clear Walls");
		if (showControls) {
			main.add(blankWallsButton);
		}
		blankWallsButton.addActionListener(this);
		
		// Initial blankButton
		borderButton = new Button("Add Border");
		if (showControls) {
			main.add(borderButton);
		}
		borderButton.addActionListener(this);
		
		// Initial exportButton
//		exportButton = new Button("Import/Export");
//		if (showControls) {
//			main.add(exportButton);
//		}
//		exportButton.addActionListener(this);
		
		// Initial stoppedCheck
		stoppedCheck = new Checkbox("Stopped");
		stoppedCheck.addItemListener(this);
		if (showControls) {
			main.add(stoppedCheck);
		}
		
		// Initial stoppedCheck		
		fixedEndsCheck = new Checkbox("Fixed Edges", true);
		fixedEndsCheck.addItemListener(this);
		if (showControls) {
			main.add(fixedEndsCheck);
		}

		// Initial view3dCheck
		view3dCheck = new Checkbox("3-D View");
		view3dCheck.addItemListener(this);
		if (showControls) {
			main.add(view3dCheck);
		}

		// Initial speedBar
		Label l = new Label("Simulation Speed", Label.CENTER);
		speedBar = new Scrollbar(Scrollbar.HORIZONTAL, 8, 1, 1, 100); // Default: 8/100
		speedBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(speedBar);
		}

		// Initial resBar
		l = new Label("Resolution", Label.CENTER);
//		resBar = new Scrollbar(Scrollbar.HORIZONTAL, res, 5, 5, 400);
		resBar = new Scrollbar(Scrollbar.HORIZONTAL, 110, 5, 5, 400); // Default: 110/400
		resBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(resBar);
		}
		setResolution();

		// Initial dampingBar
		l = new Label("Damping", Label.CENTER);
		dampingBar = new Scrollbar(Scrollbar.HORIZONTAL, 10, 1, 2, 100); // Default: 10/100
		dampingBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(dampingBar);
		}

		// Initial freqBar
		l = new Label("Source Frequency", Label.CENTER);
		freqBar = new Scrollbar(Scrollbar.HORIZONTAL, freqBarValue = 15, 1, 1, 30); // Default: 15/30
		freqBar.addAdjustmentListener(this);
		if (showControls) {
			main.add(l);
			main.add(freqBar);
		}

		// Initial brightnessBar
		l = new Label("Brightness", Label.CENTER);
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

//		if (showControls)
//			main.add(new Label("http://www.falstad.com"));

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
					if (setupList.elementAt(i).getClass().getName().equalsIgnoreCase("RippleFrame$" + param))
						break;
				}
				if (i != setupList.size()) {
					setupChooser.select(i);
				}
			}
			
			for (i = 0; i != 20; i++) {
				param = applet.getParameter("colorScheme" + (i + 1));
				if (param == null)
					break;
				decodeColorScheme(i, param);
			}
		} catch (Exception e) {
			if (applet != null)
				e.printStackTrace();
		}
		if (colorChooser.getItemCount() == 0) {
			addDefaultColorScheme();
		}
		doColor();
//		random = new Random();
		setDamping();
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
		medium = new int[gridSizeXY];
		damp = new float[gridSizeXY];
		exceptional = new boolean[gridSizeXY];
		int i, j;
		for (i = 0; i != gridSizeXY; i++)
			damp[i] = 1f; // (float) dampcoef;
		for (i = 0; i != windowOffsetX; i++)
			for (j = 0; j != gridSizeX; j++)
				damp[i + j * gw] = damp[gridSizeX - 1 - i + gw * j] = damp[j + gw * i] = damp[j + (gridSizeY - 1 - i) * gw] = (float) (.999 - (windowOffsetX - i) * .002);
		if (setup)
			doSetup();

	}

	boolean shown = false;

	public void triggerShow() {
		if (!shown)
			show();
		shown = true;
	}

	void handleResize() {
		Dimension d = winSize = cv.getSize();
		if (winSize.width == 0)
			return;
		pixels = null;
		if (useBufferedImage) {
			try {
				/*
				 * simulate the following code using reflection: dbimage = new
				 * BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
				 * DataBuffer db = (DataBuffer)(((BufferedImage)dbimage).
				 * getRaster().getDataBuffer()); DataBufferInt dbi =
				 * (DataBufferInt) db; pixels = dbi.getData();
				 */
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
				// ee.printStackTrace();
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
		if (applet == null)
			dispose();
		else
			applet.destroyFrame();
	}

	void doBlank() {
		int x, y;
		// I set all the elements in the grid to 1e-10 instead of 0 because
		// if I set them to zero, then the simulation slows down for a
		// short time until the grid fills up again. Don't ask me why!
		// I don't know. This showed up when I started using floats
		// instead of doubles.
		for (x = 0; x != gridSizeXY; x++)
			func[x] = funci[x] = 1e-10f;
	}

	void doBlankWalls() {
		int x, y;
		for (x = 0; x != gridSizeXY; x++) {
			walls[x] = false;
			medium[x] = 0;
		}
		calcExceptions();
	}

	void doBorder() {
		int x, y;
		for (x = 0; x < gridSizeX; x++) {
			setWall(x, windowOffsetY);
			setWall(x, windowBottom);
		}
		for (y = 0; y < gridSizeY; y++) {
			setWall(windowOffsetX, y);
			setWall(windowRight, y);
		}
		calcExceptions();
	}

	void setWall(int x, int y) {
		walls[x + gw * y] = true;
	}

	void setWall(int x, int y, boolean b) {
		walls[x + gw * y] = b;
	}

	void setMedium(int x, int y, int q) {
		medium[x + gw * y] = q;
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

	void calcExceptions() {
		int x, y;
		// if walls are in place on border, need to extend that through
		// hidden area to avoid "leaks"
		for (x = 0; x != gridSizeX; x++)
			for (y = 0; y < windowOffsetY; y++) {
				walls[x + gw * y] = walls[x + gw * windowOffsetY];
				walls[x + gw * (gridSizeY - y - 1)] = walls[x + gw * (gridSizeY - windowOffsetY - 1)];
			}
		for (y = 0; y < gridSizeY; y++)
			for (x = 0; x < windowOffsetX; x++) {
				walls[x + gw * y] = walls[windowOffsetX + gw * y];
				walls[gridSizeX - x - 1 + gw * y] = walls[gridSizeX - windowOffsetX - 1 + gw * y];
			}
		// generate exceptional array, which is useful for doing
		// special handling of elements
		for (x = 1; x < gridSizeX - 1; x++)
			for (y = 1; y < gridSizeY - 1; y++) {
				int gi = x + gw * y;
				exceptional[gi] = walls[gi - 1] || walls[gi + 1] || walls[gi - gw] || walls[gi + gw] || walls[gi] || medium[gi] != medium[gi - 1] || medium[gi] != medium[gi + 1];
				if ((x == 1 || x == gridSizeX - 2) && medium[gi] != medium[gridSizeX - 1 - x + gw * (y + 1)] || medium[gi] != medium[gridSizeX - 1 - x + gw * (y - 1)])
					exceptional[gi] = true;
			}
		// put some extra exceptions at the corners to ensure tadd2, sinth,
		// etc get calculated
		exceptional[1 + gw] = exceptional[gridSizeX - 2 + gw] = exceptional[1 + (gridSizeY - 2) * gw] = exceptional[gridSizeX - 2 + (gridSizeY - 2) * gw] = true;
	}

	void centerString(Graphics g, String s, int y) {
		FontMetrics fm = g.getFontMetrics();
		g.drawString(s, (winSize.width - fm.stringWidth(s)) / 2, y);
	}

	public void paint(Graphics g) {
		cv.repaint();
	}

	long lastTime = 0, lastFrameTime, secTime = 0;
	int frames = 0;
	int steps = 0;
	int framerate = 0, steprate = 0;

	boolean moveRight = true;
	boolean moveDown = true;

	public void updateRipple(Graphics realg) {
		if (winSize == null || winSize.width == 0) {
			// this works around some weird bug in IE which causes the
			// applet to not show up properly sometimes.
			handleResize();
			return;
		}
		
		long sysTime = getTimeMillis();
		
		if (increaseResolution) {
			increaseResolution = false;
			if (resBar.getValue() < 495)
				setResolution(resBar.getValue() + 10);
		}
		
		double tadd = 0;
		if (!stoppedCheck.getState()) {
			int val = 5; // speedBar.getValue();
			tadd = val * .05;
		}

		boolean stopFunc = dragging && selectedSource == -1 && view3dCheck.getState() == false && modeChooser.getSelectedIndex() == MODE_SETFUNC;
		if (stoppedCheck.getState())
			stopFunc = true;
		
		int iterCount = speedBar.getValue();
		if (!stopFunc) {
			/*
			 * long sysTime = System.currentTimeMillis(); if (sysTime-secTime >=
			 * 1000) { framerate = frames; steprate = steps; frames = 0; steps =
			 * 0; secTime = sysTime; } lastTime = sysTime;
			 */
			int iter;
			int mxx = gridSizeX - 1;
			int mxy = gridSizeY - 1;
			for (iter = 0; iter != iterCount; iter++) {
				int jstart, jend, jinc;
				if (moveDown) {
					// we process the rows in alternate directions
					// each time to avoid any directional bias.
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
				int curMedium = -1;
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
						// calculate equilibrum point of this
						// element's oscillation
						float previ = func[gi - 1];
						float nexti = func[gi + 1];
						float prevj = func[gi - gw];
						float nextj = func[gi + gw];
						float basis = (nexti + previ + nextj + prevj) * .25f;
						if (exceptional[gi]) {
							if (curMedium != medium[gi]) {
								curMedium = medium[gi];
								double tadd2 = tadd * (1 - (mediumMaxIndex / mediumMax) * curMedium);
								sinhalfth = (float) Math.sin(tadd2 / 2);
								sinth = (float) (Math.sin(tadd2) * dampcoef);
								scaleo = (float) (1 - Math.sqrt(4 * sinhalfth * sinhalfth - sinth * sinth));
							}
							if (walls[gi])
								continue;
							int count = 4;
							if (fixedEndsCheck.getState()) {
								if (walls[gi - 1])
									previ = 0;
								if (walls[gi + 1])
									nexti = 0;
								if (walls[gi - gw])
									prevj = 0;
								if (walls[gi + gw])
									nextj = 0;
							} else {
								if (walls[gi - 1])
									previ = walls[gi + 1] ? func[gi] : func[gi + 1];
								if (walls[gi + 1])
									nexti = walls[gi - 1] ? func[gi] : func[gi - 1];
								if (walls[gi - gw])
									prevj = walls[gi + gw] ? func[gi] : func[gi + gw];
								if (walls[gi + gw])
									nextj = walls[gi - gw] ? func[gi] : func[gi - gw];
							}
							basis = (nexti + previ + nextj + prevj) * .25f;
						}
						// what we are doing here (aside from damping)
						// is rotating the point (func[gi], funci[gi])
						// an angle tadd about the point (basis, 0).
						// Rather than call atan2/sin/cos, we use this
						// faster method using some precomputed info.
						float a = 0;
						float b = 0;
						if (damp[gi] == 1f) {
							a = func[gi] - basis;
							b = funci[gi];
						} else {
							a = (func[gi] - basis) * damp[gi];
							b = funci[gi] * damp[gi];
						}
						func[gi] = basis + a * scaleo - b * sinth;
						funci[gi] = b * scaleo + a * sinth;
					}
				}
				t += tadd;
				if (sourceCount > 0) {
					double w = freqBar.getValue() * (t - freqTimeZero) * freqMult;
					double w2 = w;
					boolean skip = false;
					switch (auxFunction) {
					case AUX_FREQ:
						w2 = auxBar.getValue() * t * freqMult;
						break;
					case AUX_PHASE:
						w2 = w + (auxBar.getValue() - 1) * (pi / 29);
						break;
					}
					double v = 0;
					double v2 = 0;
					switch (sourceWaveform) {
					case SWF_SIN:
						v = Math.cos(w);
						if (sourceCount >= (sourcePlane ? 4 : 2))
							v2 = Math.cos(w2);
						else if (sourceFreqCount == 2)
							v = (v + Math.cos(w2)) * .5;
						break;
					case SWF_SQUARE:
						w %= pi * 2;
						v = (w < pi) ? 1 : -1;
						break;
					case SWF_PULSE: {
						w %= pi * 2;
						double pulselen = pi / 4;
						double pulselen2 = freqBar.getValue() * .2;
						if (pulselen2 < pulselen)
							pulselen = pulselen2;
						v = (w > pulselen) ? 0 : Math.sin(w * pi / pulselen);
						if (w > pulselen * 2)
							skip = true;
					}
						break;
					}
					for (int j = 0; j != sourceCount; j++) {
						if ((j % 2) == 0)
							sources[j].v = (float) (v * setup.sourceStrength());
						else
							sources[j].v = (float) (v2 * setup.sourceStrength());
					}
					if (sourcePlane) {
						if (!skip) {
							for (int j = 0; j != sourceCount / 2; j++) {
								OscSource src1 = sources[j * 2];
								OscSource src2 = sources[j * 2 + 1];
								OscSource src3 = sources[j];
								drawPlaneSource(src1.x, src1.y, src2.x, src2.y, src3.v, w);
							}
						}
					} else {
						if (sourceMoving) {
							int sy;
							movingSourcePos += tadd * .02 * auxBar.getValue();
							double wm = movingSourcePos;
							int h = windowHeight - 3;
							wm %= h * 2;
							sy = (int) wm;
							if (sy > h)
								sy = 2 * h - sy;
							sy += windowOffsetY + 1;
							sources[0].y = sy;
						}
						for (int i = 0; i != sourceCount; i++) {
							OscSource src = sources[i];
							func[src.x + gw * src.y] = src.v;
							funci[src.x + gw * src.y] = 0;
						}
					}
				}
				setup.eachFrame();
				steps++;
				filterGrid();
			}
		}

		brightMult = Math.exp(brightnessBar.getValue() / 100. - 5.);
		if (view3dCheck.getState())
			draw3dView();
		else
			draw2dView();

		if (imageSource != null)
			imageSource.newPixels();

		realg.drawImage(dbimage, 0, 0, this);
		if (dragStartX >= 0 && !view3dCheck.getState()) {
			int x = dragStartX * windowWidth / winSize.width;
			int y = windowHeight - 1 - (dragStartY * windowHeight / winSize.height);
			String s = "(" + x + "," + y + ")";
			realg.setColor(Color.white);
			FontMetrics fm = realg.getFontMetrics();
			int h = 5 + fm.getAscent();
			realg.fillRect(0, winSize.height - h, fm.stringWidth(s) + 10, h);
			realg.setColor(Color.black);
			realg.drawString(s, 5, winSize.height - 5);
		}

		/*
		 * frames++; realg.setColor(Color.white); realg.drawString("Framerate: "
		 * + framerate, 10, 10); realg.drawString("Steprate: " + steprate, 10,
		 * 30); lastFrameTime = lastTime;
		 */

		if (!stoppedCheck.getState()) {
			long diff = getTimeMillis() - sysTime;
			// we want the time it takes for a wave to travel across the screen
			// to be more-or-less constant, but don't do anything after 5
			// seconds
			if (adjustResolution && diff > 0 && sysTime < startTime + 1000 && windowOffsetX * diff / iterCount < 55) {
				increaseResolution = true;
				startTime = sysTime;
			}
			if (dragging && selectedSource == -1 && modeChooser.getSelectedIndex() == MODE_FUNCHOLD)
				editFuncPoint(dragX, dragY);
			cv.repaint(0);
		}
	}

	// filter out high-frequency noise
	int filterCount;

	void filterGrid() {
		int x, y;
		if (fixedEndsCheck.getState())
			return;
		if (sourceCount > 0 && freqBarValue > 23)
			return;
		if (sourceFreqCount >= 2 && auxBar.getValue() > 23)
			return;
		if (++filterCount < 10)
			return;
		filterCount = 0;
		for (y = windowOffsetY; y < windowBottom; y++)
			for (x = windowOffsetX; x < windowRight; x++) {
				int gi = x + y * gw;
				if (walls[gi])
					continue;
				if (func[gi - 1] < 0 && func[gi] > 0 && func[gi + 1] < 0 && !walls[gi + 1] && !walls[gi - 1])
					func[gi] = (func[gi - 1] + func[gi + 1]) / 2;
				if (func[gi - gw] < 0 && func[gi] > 0 && func[gi + gw] < 0 && !walls[gi - gw] && !walls[gi + gw])
					func[gi] = (func[gi - gw] + func[gi + gw]) / 2;
				if (func[gi - 1] > 0 && func[gi] < 0 && func[gi + 1] > 0 && !walls[gi + 1] && !walls[gi - 1])
					func[gi] = (func[gi - 1] + func[gi + 1]) / 2;
				if (func[gi - gw] > 0 && func[gi] < 0 && func[gi + gw] > 0 && !walls[gi - gw] && !walls[gi + gw])
					func[gi] = (func[gi - gw] + func[gi + gw]) / 2;
			}
	}

	void plotPixel(int x, int y, int pix) {
		if (x < 0 || x >= winSize.width)
			return;
		try {
			pixels[x + y * winSize.width] = pix;
		} catch (Exception e) {
		}
	}

	// draw a circle the slow and dirty way
	void plotSource(int n, int xx, int yy) {
		int rad = sourceRadius;
		int j;
		int col = (sourceColor.getRed() << 16) | (sourceColor.getGreen() << 8) | (sourceColor.getBlue()) | 0xFF000000;
		if (n == selectedSource)
			col ^= 0xFFFFFF;
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

	void draw2dView() {
		int ix = 0;
		int i, j, k, l;
		for (j = 0; j != windowHeight; j++) {
			ix = winSize.width * (j * winSize.height / windowHeight);
			int j2 = j + windowOffsetY;
			int gi = j2 * gw + windowOffsetX;
			int y = j * winSize.height / windowHeight;
			int y2 = (j + 1) * winSize.height / windowHeight;
			for (i = 0; i != windowWidth; i++, gi++) {
				int x = i * winSize.width / windowWidth;
				int x2 = (i + 1) * winSize.width / windowWidth;
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
					double d3 = medium[gi] * (1 / 255.01);
					double d4 = 1 - d3;
					double a1 = d1 * d4;
					double a2 = d2 * d4;
					colR = (int) (negColor.getRed() * a1 + zeroColor.getRed() * a2);
					colG = (int) (negColor.getGreen() * a1 + zeroColor.getGreen() * a2);
					colB = (int) (negColor.getBlue() * a1 + zeroColor.getBlue() * a2);
				} else {
					double d1 = dy;
					double d2 = 1 - dy;
					double d3 = medium[gi] * (1 / 255.01);
					double d4 = 1 - d3;
					double a1 = d1 * d4;
					double a2 = d2 * d4;
					colR = (int) (posColor.getRed() * a1 + zeroColor.getRed() * a2);
					colG = (int) (posColor.getGreen() * a1 + zeroColor.getGreen() * a2);
					colB = (int) (posColor.getBlue() * a1 + zeroColor.getBlue() * a2);	
				}
				col = (255 << 24) | (colR << 16) | (colG << 8) | (colB);
				for (k = 0; k != x2 - x; k++, ix++)
					for (l = 0; l != y2 - y; l++)
						pixels[ix + l * winSize.width] = col;
			}
		}
		int intf = (gridSizeY / 2 - windowOffsetY) * winSize.height / windowHeight;
		for (i = 0; i != sourceCount; i++) {
			OscSource src = sources[i];
			int xx = src.getScreenX();
			int yy = src.getScreenY();
			plotSource(i, xx, yy);
		}
	}

	double realxmx, realxmy, realymz, realzmy, realzmx, realymadd, realzmadd;
	double viewAngle = pi, viewAngleDragStart;
	double viewZoom = .775, viewZoomDragStart;
	double viewAngleCos = -1, viewAngleSin = 0;
	double viewHeight = -38, viewHeightDragStart;
	double scalex, scaley;
	int centerX3d, centerY3d;
	int xpoints[] = new int[4], ypoints[] = new int[4];
	final double viewDistance = 66;

	void map3d(double x, double y, double z, int xpoints[], int ypoints[], int pt) {
		/*
		 * x *= aspectRatio; z *= -4; x *= 16./sampleCount; y *=
		 * 16./sampleCount; double realx = x*viewAngleCos + y*viewAngleSin; //
		 * range: [-10,10] double realy = z-viewHeight; double realz =
		 * y*viewAngleCos - x*viewAngleSin + viewDistance;
		 */
		double realx = realxmx * x + realxmy * y;
		double realy = realymz * z + realymadd;
		double realz = realzmx * x + realzmy * y + realzmadd;
		xpoints[pt] = centerX3d + (int) (realx / realz);
		ypoints[pt] = centerY3d - (int) (realy / realz);
	}

	double scaleMult;

	void scaleworld() {
		scalex = viewZoom * (winSize.width / 4) * viewDistance / 8;
		scaley = -scalex;
		int y = (int) (scaley * viewHeight / viewDistance);
		/*
		 * centerX3d = winSize.x + winSize.width/2; centerY3d = winSize.y +
		 * winSize.height/2 - y;
		 */
		centerX3d = winSize.width / 2;
		centerY3d = winSize.height / 2 - y;
		scaleMult = 16. / (windowWidth / 2);
		realxmx = -viewAngleCos * scaleMult * scalex;
		realxmy = viewAngleSin * scaleMult * scalex;
		realymz = -brightMult * scaley;
		realzmy = viewAngleCos * scaleMult;
		realzmx = viewAngleSin * scaleMult;
		realymadd = -viewHeight * scaley;
		realzmadd = viewDistance;
	}

	void draw3dView() {
		int half = gridSizeX / 2;
		scaleworld();
		int x, y;
		int xdir, xstart, xend;
		int ydir, ystart, yend;
		int sc = windowRight - 1;

		// figure out what order to render the grid elements so that
		// the ones in front are rendered first.
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

		for (x = 0; x != winSize.width * winSize.height; x++)
			pixels[x] = 0xFF000000;
		/*
		 * double zval = 2.0/sampleCount; System.out.println(zval); if
		 * (sampleCount == 128) zval = .1;
		 */
		double zval = .1;
		double zval2 = zval * zval;

		for (x = xstart; x != xend; x += xdir) {
			for (y = ystart; y != yend; y += ydir) {
				if (!xFirst)
					x = xstart;
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
					if (xFirst)
						break;
				}
			}
			if (!xFirst)
				break;
		}
	}

	int computeColor(int gix, double c) {
		double h = func[gix] * brightMult;
		if (c < 0)
			c = 0;
		if (c > 1)
			c = 1;
		c = .5 + c * .5;
		double redness = (h < 0) ? -h : 0;
		double grnness = (h > 0) ? h : 0;
		if (redness > 1)
			redness = 1;
		if (grnness > 1)
			grnness = 1;
		if (grnness < 0)
			grnness = 0;
		if (redness < 0)
			redness = 0;
		double grayness = (1 - (redness + grnness)) * c;
		double grayness2 = grayness;
		if (medium[gix] > 0) {
			double mm = 1 - (medium[gix] * (1 / 255.01));
			grayness2 *= mm;
		}
		double gray = .6;
		int ri = (int) ((c * redness + gray * grayness2) * 255);
		int gi = (int) ((c * grnness + gray * grayness2) * 255);
		int bi = (int) ((gray * grayness) * 255);
		return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
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

	int interp(int x1, int y1, int x2, int y2, int x) {
		if (x1 == x2)
			return y1;
		if (x < x1 && x < x2 || x > x1 && x > x2)
			System.out.print("interp out of bounds\n");
		return (int) (y1 + ((double) x - x1) * (y2 - y1) / (x2 - x1));
	}

	void fillTriangle1(int x1, int y1, int x2, int y2, int y3, int col) {
		// x2 == x3
		int dir = (x1 > x2) ? -1 : 1;
		int x = x1;
		if (x < 0) {
			x = 0;
			if (x2 < 0)
				return;
		}
		if (x >= winSize.width) {
			x = winSize.width - 1;
			if (x2 >= winSize.width)
				return;
		}
		if (y2 > y3) {
			int q = y2;
			y2 = y3;
			y3 = q;
		}
		// y2 < y3
		while (x != x2 + dir) {
			// XXX this could be speeded up
			int ya = interp(x1, y1, x2, y2, x);
			int yb = interp(x1, y1, x2, y3, x);
			if (ya < 0)
				ya = 0;
			if (yb >= winSize.height)
				yb = winSize.height - 1;

			for (; ya <= yb; ya++)
				pixels[x + ya * winSize.width] = col;
			x += dir;
			if (x < 0 || x >= winSize.width)
				return;
		}
	}

	int abs(int x) {
		return x < 0 ? -x : x;
	}

	void drawPlaneSource(int x1, int y1, int x2, int y2, float v, double w) {
		if (y1 == y2) {
			if (x1 == windowOffsetX)
				x1 = 0;
			if (x2 == windowOffsetX)
				x2 = 0;
			if (x1 == windowOffsetX + windowWidth - 1)
				x1 = gridSizeX - 1;
			if (x2 == windowOffsetX + windowWidth - 1)
				x2 = gridSizeX - 1;
		}
		if (x1 == x2) {
			if (y1 == windowOffsetY)
				y1 = 0;
			if (y2 == windowOffsetY)
				y2 = 0;
			if (y1 == windowOffsetY + windowHeight - 1)
				y1 = gridSizeY - 1;
			if (y2 == windowOffsetY + windowHeight - 1)
				y2 = gridSizeY - 1;
		}

		/*
		 * double phase = 0; if (sourceChooser.getSelectedIndex() ==
		 * SRC_1S1F_PLANE_PHASE) phase =
		 * (auxBar.getValue()-15)*3.8*freqBar.getValue()*freqMult;
		 */

		// need to draw a line from x1,y1 to x2,y2
		if (x1 == x2 && y1 == y2) {
			func[x1 + gw * y1] = v;
			funci[x1 + gw * y1] = 0;
		} else if (abs(y2 - y1) > abs(x2 - x1)) {
			// y difference is greater, so we step along y's
			// from min to max y and calculate x for each step
			double sgn = sign(y2 - y1);
			int x, y;
			for (y = y1; y != y2 + sgn; y += sgn) {
				x = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
				double ph = sgn * (y - y1) / (y2 - y1);
				int gi = x + gw * y;
				func[gi] = setup.calcSourcePhase(ph, v, w);
				// (phase == 0) ? v :
				// (float) (Math.sin(w+ph));
				funci[gi] = 0;
			}
		} else {
			// x difference is greater, so we step along x's
			// from min to max x and calculate y for each step
			double sgn = sign(x2 - x1);
			int x, y;
			for (x = x1; x != x2 + sgn; x += sgn) {
				y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
				double ph = sgn * (x - x1) / (x2 - x1);
				int gi = x + gw * y;
				func[gi] = setup.calcSourcePhase(ph, v, w);
				// (phase == 0) ? v :
				// (float) (Math.sin(w+ph));
				funci[gi] = 0;
			}
		}
	}

	int sign(int x) {
		return (x < 0) ? -1 : (x == 0) ? 0 : 1;
	}

	void edit(MouseEvent e) {
		if (view3dCheck.getState())
			return;
		int x = e.getX();
		int y = e.getY();
		if (selectedSource != -1) {
			x = x * windowWidth / winSize.width;
			y = y * windowHeight / winSize.height;
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
				// y difference is greater, so we step along y's
				// from min to max y and calculate x for each step
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
				// x difference is greater, so we step along x's
				// from min to max x and calculate y for each step
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
		int xp = x * windowWidth / winSize.width + windowOffsetX;
		int yp = y * windowHeight / winSize.height + windowOffsetY;
		int gi = xp + yp * gw;
		if (modeChooser.getSelectedIndex() == MODE_WALLS) {
			if (!dragSet && !dragClear) {
				dragClear = walls[gi];
				dragSet = !dragClear;
			}
			walls[gi] = dragSet;
			calcExceptions();
			func[gi] = funci[gi] = 0;
		} else if (modeChooser.getSelectedIndex() == MODE_MEDIUM) {
			if (!dragSet && !dragClear) {
				dragClear = medium[gi] > 0;
				dragSet = !dragClear;
			}
			medium[gi] = (dragSet) ? mediumMax : 0;
			calcExceptions();
		} else {
			if (!dragSet && !dragClear) {
				dragClear = func[gi] > .1;
				dragSet = !dragClear;
			}
			func[gi] = (dragSet) ? 1 : -1;
			funci[gi] = 0;
		}
		cv.repaint(0);
	}

	void selectSource(MouseEvent me) {
		int x = me.getX();
		int y = me.getY();
		int i;
		for (i = 0; i != sourceCount; i++) {
			OscSource src = sources[i];
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

	void setDamping() {
		/*
		 * int i; double damper = dampingBar.getValue() * .00002;// was 5
		 * dampcoef = Math.exp(-damper);
		 */
		dampcoef = 1;
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		cv.repaint();
	}

	public void componentResized(ComponentEvent e) {
		handleResize();
		cv.repaint(100);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == blankButton) {
			doBlank();
			cv.repaint();
		}
		if (e.getSource() == blankWallsButton) {
			doBlankWalls();
			cv.repaint();
		}
		if (e.getSource() == borderButton) {
			doBorder();
			cv.repaint();
		}
//		if (e.getSource() == exportButton)
//			doImport();
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		System.out.print(((Scrollbar) e.getSource()).getValue() + "\n");
		if (e.getSource() == resBar) {
			setResolution();
			reinit();
		}
		if (e.getSource() == dampingBar)
			setDamping();
		if (e.getSource() == brightnessBar)
			cv.repaint(0);
		if (e.getSource() == freqBar)
			setFreq();
	}

	void setFreqBar(int x) {
		freqBar.setValue(x);
		freqBarValue = x;
		freqTimeZero = 0;
	}

	void setFreq() {
		// adjust time zero to maintain continuity in the freq func
		// even though the frequency has changed.
		double oldfreq = freqBarValue * freqMult;
		freqBarValue = freqBar.getValue();
		double newfreq = freqBarValue * freqMult;
		double adj = newfreq - oldfreq;
		freqTimeZero = t - oldfreq * (t - freqTimeZero) / newfreq;
	}

	void setResolution() {
		windowWidth = windowHeight = resBar.getValue();
		int border = windowWidth / 9;
		if (border < 20)
			border = 20;
		windowOffsetX = windowOffsetY = border;
		gridSizeX = windowWidth + windowOffsetX * 2;
		gridSizeY = windowHeight + windowOffsetY * 2;
		windowBottom = windowOffsetY + windowHeight - 1;
		windowRight = windowOffsetX + windowWidth - 1;
	}

	void setResolution(int x) {
		resBar.setValue(x);
		setResolution();
		reinit();
	}

	public void mouseDragged(MouseEvent e) {
		if (view3dCheck.getState()) {
			view3dDrag(e);
		}
		if (!dragging)
			selectSource(e);
		dragging = true;
		edit(e);
		adjustResolution = false;
		cv.repaint(0);
	}

	public void mouseMoved(MouseEvent e) {
		if (dragging)
			return;
		int x = e.getX();
		int y = e.getY();
		dragStartX = dragX = x;
		dragStartY = dragY = y;
		viewAngleDragStart = viewAngle;
		viewHeightDragStart = viewHeight;
		selectSource(e);
		if (stoppedCheck.getState())
			cv.repaint(0);
	}

	void view3dDrag(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		viewAngle = (dragStartX - x) / 40. + viewAngleDragStart;
		while (viewAngle < 0)
			viewAngle += 2 * pi;
		while (viewAngle >= 2 * pi)
			viewAngle -= 2 * pi;
		viewAngleCos = Math.cos(viewAngle);
		viewAngleSin = Math.sin(viewAngle);
		viewHeight = (dragStartY - y) / 10. + viewHeightDragStart;

		/*
		 * viewZoom = (y-dragStartY)/40. + viewZoomDragStart; if (viewZoom < .1)
		 * viewZoom = .1; System.out.println(viewZoom);
		 */
		cv.repaint();
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
		dragStartX = -1;
	}

	public void mousePressed(MouseEvent e) {
		adjustResolution = false;
		mouseMoved(e);
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0)
			return;
		dragging = true;
		edit(e);
	}

	public void mouseReleased(MouseEvent e) {
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0)
			return;
		dragging = false;
		dragSet = dragClear = false;
		cv.repaint();
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == stoppedCheck) {
			cv.repaint();
			return;
		}
		if (e.getItemSelectable() == sourceChooser) {
			if (sourceChooser.getSelectedIndex() != sourceIndex)
				setSources();
		}
		if (e.getItemSelectable() == setupChooser)
			doSetup();
		if (e.getItemSelectable() == colorChooser)
			doColor();
	}

	void doSetup() {
		t = 0;
		if (resBar.getValue() < 32)
			setResolution(32);
		doBlank();
		doBlankWalls();
		// don't use previous source positions, use defaults
		sourceCount = -1;
		sourceChooser.select(SRC_1S1F);
		dampingBar.setValue(10);
		setFreqBar(5);
		setBrightness(10);
		auxBar.setValue(1);
		fixedEndsCheck.setState(true);
		setup = (Setup) setupList.elementAt(setupChooser.getSelectedIndex());
		setup.select();
		setup.doSetupSources();
		calcExceptions();
		setDamping();
		// System.out.println("setup " + setupChooser.getSelectedIndex());
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

	void addDefaultColorScheme() {
		
		String schemes[] = { "#000000 #0029A3 #335CD6 #0033CC #CC0000", 
				"#800000 #ffffff #000000 #808080 #CC0000" };
		int i;

		for (i = 0; i != 2; i++)
			decodeColorScheme(i, schemes[i]);
	}

	void decodeColorScheme(int cn, String s) {
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			int i;
			for (i = 0; i != 5; i++)
				schemeColors[cn][i] = Color.decode(st.nextToken());
		}
		colorChooser.add("Color Scheme " + (cn + 1));
	}

	void addMedium() {
		int i, j;
		for (i = 0; i != gridSizeX; i++)
			for (j = gridSizeY / 2; j != gridSizeY; j++)
				medium[i + j * gw] = mediumMax;
	}

	void setSources() {
		int oldSCount = sourceCount;
		boolean oldPlane = sourcePlane;
		
		sourceIndex = sourceChooser.getSelectedIndex();
//		sourcePlane = (sourceChooser.getSelectedIndex() >= SRC_1S1F_PLANE && sourceChooser.getSelectedIndex() < SRC_6S1F);
		sourcePlane = (sourceChooser.getSelectedIndex() >= SRC_1S1F_PLANE && sourceChooser.getSelectedIndex() <= SRC_2S1F_PLANE );
		
		sourceCount = 1;
		sourceFreqCount = 1;
		sourceMoving = false;
		sourceWaveform = SWF_SIN;
//		boolean phase = false;
		
		
		switch (sourceChooser.getSelectedIndex()) {
//		case 0:
//			sourceCount = 0;
//			break;
//		case 2:
//			sourceFreqCount = 2;
//			break;
//		case 3:
//			sourceCount = 2;
//			break;
//		case 4:
//			sourceCount = 2;
//			sourceFreqCount = 2;
//			break;
//		case 5:
//			sourceCount = 3;
//			break;
//		case 6:
//			sourceCount = 4;
//			break;
//		case 7:
//			sourceWaveform = SWF_SQUARE;
//			break;
//		case 8:
//			sourceWaveform = SWF_PULSE;
//			break;
//		case 9:
//			sourceMoving = true;
//			break;
//		case 11:
//			sourceFreqCount = 2;
//			break;
//		case 12:
//			sourceCount = 2;
//			break;
//		case 13:
//			sourceCount = sourceFreqCount = 2;
//			break;
//		case 14:
//			sourceWaveform = SWF_PULSE;
//			break;
//		case 15:
//			phase = true;
//			break;
//		case 16:
//			sourceCount = 6;
//			break;
//		case 17:
//			sourceCount = 8;
//			break;
//		case 18:
//			sourceCount = 10;
//			break;
//		case 19:
//			sourceCount = 12;
//			break;
//		case 20:
//			sourceCount = 16;
//			break;
//		case 21:
//			sourceCount = 20;
//			break;
//		}
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
		
//		if (sourceFreqCount >= 2) {
//			auxFunction = AUX_FREQ;
//			auxBar.setValue(freqBar.getValue());
//			if (sourceCount == 2)
//				auxLabel.setText("Source 2 Frequency");
//			else
//				auxLabel.setText("2nd Frequency");
//		} else if (sourceCount == 2 || sourceCount >= 4 || phase) {
//			auxFunction = AUX_PHASE;
//			auxBar.setValue(1);
//			auxLabel.setText("Phase Difference");
//		} else if (sourceMoving) {
//			auxFunction = AUX_SPEED;
//			auxBar.setValue(7);
//			auxLabel.setText("Source Speed");
//		} else {
//			auxFunction = AUX_NONE;
//			auxBar.hide();
//			auxLabel.hide();
//		}
		if (sourceMoving) {
			auxFunction = AUX_SPEED;
			auxBar.setValue(7);
			auxLabel.setText("Source Speed");
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
				sources[0] = new OscSource(windowOffsetX, windowOffsetY + 1);
				sources[1] = new OscSource(x2, windowOffsetY + 1);
				sources[2] = new OscSource(windowOffsetX, y2);
				sources[3] = new OscSource(x2, y2);
			}
		} else if (!(!oldPlane && oldSCount == sourceCount)) {
			sources[0] = new OscSource(gridSizeX / 2, windowOffsetY + 1);
			sources[1] = new OscSource(gridSizeX / 2, gridSizeY - windowOffsetY - 2);
			sources[2] = new OscSource(windowOffsetX + 1, gridSizeY / 2);
			sources[3] = new OscSource(gridSizeX - windowOffsetX - 2, gridSizeY / 2);
//			for (int i = 4; i < sourceCount; i++) {
//				sources[i] = new OscSource(windowOffsetX + 1 + i * 2, gridSizeY / 2);
//			}
		}
	}

	class OscSource {
		int x;
		int y;
		float v;

		OscSource(int xx, int yy) {
			x = xx;
			y = yy;
		}

		int getScreenX() {
			return ((x - windowOffsetX) * winSize.width + winSize.width / 2) / windowWidth;
		}

		int getScreenY() {
			return ((y - windowOffsetY) * winSize.height + winSize.height / 2) / windowHeight;
		}
	};

//	void doImport() {
//		if (impDialog != null) {
//			requestFocus();
//			impDialog.setVisible(false);
//			impDialog = null;
//		}
//		String dump = "";
//
//		int i;
//		dump = "$ 0 " + resBar.getValue() + " " + sourceChooser.getSelectedIndex() + " " + colorChooser.getSelectedIndex() + " " + fixedEndsCheck.getState() + " " + view3dCheck.getState() + " " + speedBar.getValue() + " " + freqBar.getValue() + " " + brightnessBar.getValue() + " " + auxBar.getValue() + "\n";
//		for (i = 0; i != sourceCount; i++) {
//			OscSource src = sources[i];
//			dump += "s " + src.x + " " + src.y + "\n";
//		}
//		for (i = 0; i != gridSizeXY;) {
//			if (i >= gridSizeX) {
//				int istart = i;
//				for (; i < gridSizeXY && walls[i] == walls[i - gridSizeX] && medium[i] == medium[i - gridSizeX]; i++)
//					;
//				if (i > istart) {
//					dump += "l " + (i - istart) + "\n";
//					continue;
//				}
//			}
//			boolean x = walls[i];
//			int m = medium[i];
//			int ct = 0;
//			for (; i < gridSizeXY && walls[i] == x && medium[i] == m; ct++, i++)
//				;
//			dump += (x ? "w " : "c ") + ct + " " + m + "\n";
//		}
//		impDialog = new ImportDialog(this, dump);
//		impDialog.show();
//	}
//
//	void readImport(String s) {
//		byte b[] = s.getBytes();
//		int len = s.length();
//		int p;
//		int x = 0;
//		int srci = 0;
//		setupChooser.select(0);
//		setup = (Setup) setupList.elementAt(0);
//		for (p = 0; p < len;) {
//			int l;
//			int linelen = 0;
//			for (l = 0; l != len - p; l++)
//				if (b[l + p] == '\n' || b[l + p] == '\r') {
//					linelen = l++;
//					if (l + p < b.length && b[l + p] == '\n')
//						l++;
//					break;
//				}
//			String line = new String(b, p, linelen);
//			StringTokenizer st = new StringTokenizer(line);
//			while (st.hasMoreTokens()) {
//				String type = st.nextToken();
//				int tint = type.charAt(0);
//				try {
//					if (tint == '$') {
//						int flags = new Integer(st.nextToken()).intValue();
//
//						resBar.setValue(new Integer(st.nextToken()).intValue());
//						setResolution();
//						reinit(false);
//
//						sourceChooser.select(new Integer(st.nextToken()).intValue());
//						setSources();
//
//						colorChooser.select(new Integer(st.nextToken()).intValue());
//						doColor();
//
//						fixedEndsCheck.setState(st.nextToken().compareTo("true") == 0);
//						view3dCheck.setState(st.nextToken().compareTo("true") == 0);
//						speedBar.setValue(new Integer(st.nextToken()).intValue());
//						freqBar.setValue(new Integer(st.nextToken()).intValue());
//						brightnessBar.setValue(new Integer(st.nextToken()).intValue());
//						auxBar.setValue(new Integer(st.nextToken()).intValue());
//						break;
//					}
//					if (tint == 'w' || tint == 'c') {
//						boolean w = (tint == 'w');
//						int ct = new Integer(st.nextToken()).intValue();
//						int md = new Integer(st.nextToken()).intValue();
//						for (; ct > 0; ct--, x++) {
//							walls[x] = w;
//							medium[x] = md;
//						}
//						break;
//					}
//					if (tint == 'l') {
//						int ct = new Integer(st.nextToken()).intValue();
//						for (; ct > 0; ct--, x++) {
//							walls[x] = walls[x - gridSizeX];
//							medium[x] = medium[x - gridSizeX];
//						}
//						break;
//					}
//					if (tint == 's') {
//						int sx = new Integer(st.nextToken()).intValue();
//						int sy = new Integer(st.nextToken()).intValue();
//						sources[srci].x = sx;
//						sources[srci].y = sy;
//						srci++;
//						break;
//					}
//					System.out.println("unknown type!");
//				} catch (Exception ee) {
//					ee.printStackTrace();
//					break;
//				}
//				break;
//			}
//			p += l;
//
//		}
//		calcExceptions();
//		setDamping();
//	}
//
//	class ImportDialogLayout implements LayoutManager {
//		public ImportDialogLayout() {
//		}
//
//		public void addLayoutComponent(String name, Component c) {
//		}
//
//		public void removeLayoutComponent(Component c) {
//		}
//
//		public Dimension preferredLayoutSize(Container target) {
//			return new Dimension(500, 500);
//		}
//
//		public Dimension minimumLayoutSize(Container target) {
//			return new Dimension(100, 100);
//		}
//
//		public void layoutContainer(Container target) {
//			Insets insets = target.insets();
//			int targetw = target.size().width - insets.left - insets.right;
//			int targeth = target.size().height - (insets.top + insets.bottom);
//			int i;
//			int pw = 300;
//			if (target.getComponentCount() == 0)
//				return;
//			Component cl = target.getComponent(target.getComponentCount() - 1);
//			Dimension dl = cl.getPreferredSize();
//			target.getComponent(0).move(insets.left, insets.top);
//			int cw = target.size().width - insets.left - insets.right;
//			int ch = target.size().height - insets.top - insets.bottom - dl.height;
//			target.getComponent(0).resize(cw, ch);
//			int h = ch + insets.top;
//			int x = 0;
//			for (i = 1; i < target.getComponentCount(); i++) {
//				Component m = target.getComponent(i);
//				if (m.isVisible()) {
//					Dimension d = m.getPreferredSize();
//					m.move(insets.left + x, h);
//					m.resize(d.width, d.height);
//					x += d.width;
//				}
//			}
//		}
//	};
//
//	class ImportDialog extends Dialog implements ActionListener {
//		RippleFrame rframe;
//		Button importButton, clearButton, closeButton;
//		TextArea text;
//
//		ImportDialog(RippleFrame f, String str) {
//			super(f, (str.length() > 0) ? "Export" : "Import", false);
//			rframe = f;
//			setLayout(new ImportDialogLayout());
//			add(text = new TextArea(str, 10, 60, TextArea.SCROLLBARS_BOTH));
//			add(importButton = new Button("Import"));
//			importButton.addActionListener(this);
//			add(clearButton = new Button("Clear"));
//			clearButton.addActionListener(this);
//			add(closeButton = new Button("Close"));
//			closeButton.addActionListener(this);
//			Point x = rframe.getLocationOnScreen();
//			resize(400, 300);
//			Dimension d = getSize();
//			setLocation(x.x + (winSize.width - d.width) / 2, x.y + (winSize.height - d.height) / 2);
//			show();
//			if (str.length() > 0)
//				text.selectAll();
//		}
//
//		public void actionPerformed(ActionEvent e) {
//			int i;
//			Object src = e.getSource();
//			if (src == importButton) {
//				rframe.readImport(text.getText());
//				setVisible(false);
//			}
//			if (src == closeButton)
//				setVisible(false);
//			if (src == clearButton)
//				text.setText("");
//		}
//
//		public boolean handleEvent(Event ev) {
//			if (ev.id == Event.WINDOW_DESTROY) {
//				rframe.requestFocus();
//				setVisible(false);
//				return true;
//			}
//			return super.handleEvent(ev);
//		}
//	}

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
			return "Single Source";
		}

		void select() {
			setFreqBar(15);
			setBrightness(27);
		}

		Setup createNext() {
			return new DoubleSourceSetup();
		}
	}

	class DoubleSourceSetup extends Setup {
		String getName() {
			return "Two Sources";
		}

		void select() {
			setFreqBar(15);
			setBrightness(19);
		}

		void doSetupSources() {
			sourceChooser.select(SRC_2S1F);
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
			return "Four Sources";
		}

		void select() {
			sourceChooser.select(SRC_4S1F);
			setFreqBar(15);
		}

		Setup createNext() {
			return new PlaneWaveSetup();
		}
	}

	class PlaneWaveSetup extends Setup {
		String getName() {
			return "Plane Wave";
		}

		void select() {
			sourceChooser.select(SRC_1S1F_PLANE);
			// setBrightness(7);
			setFreqBar(15);
		}

		Setup createNext() {
			return new IntersectingPlaneWavesSetup();
		}
	}

	class IntersectingPlaneWavesSetup extends Setup {
		String getName() {
			return "Intersecting Planes";
		}

		void select() {
			setBrightness(4);
			setFreqBar(17);
		}

		void doSetupSources() {
			sourceChooser.select(SRC_2S1F_PLANE);
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
			return "Doppler Effect 1";
		}

		void select() {
			sourceChooser.select(SRC_1S1F_MOVING);
			setFreqBar(13);
			setBrightness(20);
			fixedEndsCheck.setState(false);
		}

		Setup createNext() {
			return null;
		}
	}

}