import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Scanner;

/**
 * to note, will not work on Mac yet.
 * 
 * edit: WILL WORK ON MAC MOTHER FUCKERS
 * 
 * edit: idek if this will work on macs because app data...
 * 
 * @author Marcus
 * 
 */
public class Variable {
	private String value;
	private String filePath;
	private String fileDir;
	private static String fileExtension;
	private static String BASE_DIR = "" + System.getenv("APPDATA") + "\\";

	static {
		// first the default value is set. this is used
		// only to get the real value.
		fileExtension = "var";
		// make a new variable, for the extension
		// do not force it to be var.
		Variable var = new Variable("MAndLib\\core", "extension", "var", false);		
		// grab its value and reset the extension.
		fileExtension = var.getValue();
	}

	/**
	 * dir - where the variable file is stored. - enter things like "hjkl\\asdf"</br>
	 * </br>
	 * name - simple, name of variable file</br>
	 * </br>
	 * value - value to try and set the file to. though if it already has a
	 * value, it won't do anything</br>
	 * </br>
	 * force - if true, value will always be set to the value given, regardless
	 * of if the value is already there.</br>
	 * </br>
	 * @param dir
	 * @param name
	 * @param value
	 * @param force
	 */

	public Variable(String dir, String name, String value, boolean force) {
		

		fileDir = BASE_DIR + dir;
		filePath = BASE_DIR + dir + "\\" + name + "." + fileExtension;
		// try and load value from file, if null, screw it.
		

		String str = getValueFromFile();

		// if we could not load a value from the file
		// AKA didnt fucking exist.
		// ORRRRRRR if you were an ass, and forced
		// the value.
		if (str == null) {
			this.value = value;
			saveValue();
		} else if (force) {
			this.value = value;
			saveValue();
			// else we can load the value from the file
		} else {
			this.value = str;
		}
	}
	
	/**
	 * return the class variable for the value.
	 * shouldn't have to load it again because we
	 * always change the value locally first before we save.
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * set the value in the local class, then open a thread for saving it
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
		new Thread(new Runnable() {
			public void run() {
				saveValue();
			}
		}).start();
	}

	/**
	 * deletes and recreates the file with the new value
	 */
	private void saveValue() {
		deleteFile();
		createFile();
		try {
			// Q&D formatter to write value to current filepath.
			Formatter f = new Formatter(filePath);
			f.format("" + value);
			f.close();
		} catch (Exception e) {
			// if(weArriveHere){
			// we.are("fucked");
			// }
			e.printStackTrace();
		}
	}

	/**
	 * for refreshing the file
	 */
	private void deleteFile() {
		File f = new File(filePath);
		f.delete();
	}

	/**
	 * creates empty file at the correct path.
	 */
	private void createFile() {
		//make the directory because god knows, java can't do that for us
		//when we say we want a new file in an unknown folder noooooo....
		//jackass java
		File f = new File(fileDir);
		f.mkdirs();
		
		//no onto the file itself. create the object
		f = new File(filePath);
		try {
			//hopefully make the file...
			f.createNewFile();
		} catch (IOException e) {
			// if(weArriveHere){
			// we.are("fucked");
			// }
			e.printStackTrace();
		}
	}

	/**
	 * grab the value from the file, if nothing is there, null.
	 * 
	 * @return
	 */
	private String getValueFromFile() {
		try {
			//create the file... we don't want to do anyth stupid checking because if its
			//not perfect, we honestly don't care, just return null and reset the variable
			File f = new File(filePath);
			
			//open a scanner on the file
			Scanner s = new Scanner(f);
			
			//get the assumed value
			String str = s.nextLine();
			
			//close the file because for some reason if you don't
			//you end up dividing by zero...
			s.close();
			
			//gimme dat string
			return str;
		} catch (Exception e) {
			//dunno, don't care, reset.
			return null;
		}
	}
	
	public static void setBaseDir(String dir) {
		BASE_DIR = dir;
	}
}
