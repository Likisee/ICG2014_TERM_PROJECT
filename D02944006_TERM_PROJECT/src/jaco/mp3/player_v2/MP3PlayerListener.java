/*
 * Copyright (C) <2010> Cristian Sulea ( http://cristiansulea.entrust.ro )
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package jaco.mp3.player_v2;

import java.util.EventListener;

/**
 * The listener interface for handling player events.
 * 
 * @version 1.20, April 13, 2011
 * @author Cristian Sulea ( http://cristiansulea.entrust.ro )
 */

public interface MP3PlayerListener extends EventListener {

	/**
	 * Invoked when player start playing.
	 */
	void onPlay(MP3Player player);

	/**
	 * Invoked when player pause.
	 */
	void onPause(MP3Player player);

	/**
	 * Invoked when player stops.
	 */
	void onStop(MP3Player player);

	/**
	 * Invoked when the volume is changed.
	 */
	void onSetVolume(MP3Player player, int volume);

	/**
	 * Invoked when shuffle state of the player is changed.
	 */
	void onSetShuffle(MP3Player player, boolean shuffle);

	/**
	 * Invoked when repeat state of the player is changed.
	 */
	void onSetRepeat(MP3Player player, boolean repeat);

}
