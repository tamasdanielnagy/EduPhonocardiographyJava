package gui.general;

/**
 * Interface to notify OptionListener.
 * 
 * @author Nagy Tamas
 *
 */
public interface OptionsEventGenerator {
	
	/**
	 * Notify the listener.
	 */
	public abstract void notifyListeners();
	
	/**
	 * @param optionsListener the new options listener.
	 */
	public abstract void addOptionsListener(OptionsListener optionsListener);
}
