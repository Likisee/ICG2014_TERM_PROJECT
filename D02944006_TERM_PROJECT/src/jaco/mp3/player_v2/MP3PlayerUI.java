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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

/**
 * Java MP3 Player default UI
 * 
 * @version 2.12, April 12, 2011
 * @author Cristian Sulea ( http://cristiansulea.entrust.ro )
 */
public class MP3PlayerUI extends BasicPanelUI {

	public static ComponentUI createUI(JComponent c) {
		return new MP3PlayerUI();
	}

	//

	@Override
	public final void installUI(JComponent c) {
		super.installUI(c);
		installUI((MP3Player) c);
	}

	@Override
	public final void uninstallUI(JComponent c) {
		super.uninstallUI(c);
		uninstallUI((MP3Player) c);
	}

	//

	private JButton playButton;
	private JButton pauseButton;
	private JButton stopButton;
	private JButton skipBackwardButton;
	private JButton skipForwardButton;

	private JSlider volumeSlider;

	private JCheckBox shuffleCheckBox;
	private JCheckBox repeatCheckBox;

	private MP3PlayerListener listener;

	protected void installUI(final MP3Player player) {

		player.setOpaque(false);

		//

		playButton = new javax.swing.JButton();
		playButton.setText(">");
		playButton.setToolTipText("Play");
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.play();
			}
		});

		pauseButton = new javax.swing.JButton();
		pauseButton.setText("||");
		pauseButton.setToolTipText("Pause");
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.pause();
			}
		});

		stopButton = new javax.swing.JButton();
		stopButton.setText("#");
		stopButton.setToolTipText("Stop");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.stop();
			}
		});

		skipBackwardButton = new javax.swing.JButton();
		skipBackwardButton.setText("|<");
		skipBackwardButton.setToolTipText("Skip Backward");
		skipBackwardButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.skipBackward();
			}
		});

		skipForwardButton = new javax.swing.JButton();
		skipForwardButton.setText(">|");
		skipForwardButton.setToolTipText("Skip Forward");
		skipForwardButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.skipForward();
			}
		});

		volumeSlider = new javax.swing.JSlider();
		volumeSlider.setToolTipText("Volume");
		volumeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				player.setVolume(volumeSlider.getValue());
			}
		});
		volumeSlider.setMinimum(0);
		volumeSlider.setMaximum(100);
		volumeSlider.setMajorTickSpacing(50);
		volumeSlider.setMinorTickSpacing(10);
		volumeSlider.setPaintTicks(true);
		volumeSlider.setPaintTrack(true);

		repeatCheckBox = new javax.swing.JCheckBox();
		repeatCheckBox.setText("Repeat");
		repeatCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.setRepeat(repeatCheckBox.isSelected());
			}
		});

		shuffleCheckBox = new javax.swing.JCheckBox();
		shuffleCheckBox.setText("Shuffle");
		shuffleCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.setShuffle(shuffleCheckBox.isSelected());
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(player);
		player.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(playButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(layout.createSequentialGroup().addComponent(pauseButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(stopButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(skipBackwardButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(skipForwardButton)).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(shuffleCheckBox).addComponent(repeatCheckBox)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(volumeSlider, 0, 0, Short.MAX_VALUE))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(playButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(pauseButton).addComponent(stopButton).addComponent(skipBackwardButton).addComponent(skipForwardButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(layout.createSequentialGroup().addComponent(shuffleCheckBox).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(repeatCheckBox)).addComponent(volumeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		//

		player.addMP3PlayerListener(listener = new MP3PlayerListener() {

			@Override
			public void onPlay(MP3Player player) {}

			@Override
			public void onPause(MP3Player player) {}

			@Override
			public void onStop(MP3Player player) {}

			@Override
			public void onSetVolume(MP3Player player, int volume) {
				if (volumeSlider.getValue() != volume) {
					volumeSlider.setValue(volume);
				}
			}

			@Override
			public void onSetRepeat(MP3Player player, boolean repeat) {
				if (repeatCheckBox.isSelected() != repeat) {
					repeatCheckBox.setSelected(repeat);
				}
			}

			@Override
			public void onSetShuffle(MP3Player player, boolean shuffle) {
				if (shuffleCheckBox.isSelected() != shuffle) {
					shuffleCheckBox.setSelected(shuffle);
				}
			}
		});
	}

	protected void uninstallUI(final MP3Player player) {
		player.removeAll();
		player.removeMP3PlayerListener(listener);
	}

}
