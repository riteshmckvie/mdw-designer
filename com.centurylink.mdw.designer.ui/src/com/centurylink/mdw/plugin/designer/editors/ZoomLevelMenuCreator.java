/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.designer.editors;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ZoomLevelMenuCreator implements IMenuCreator {
    private Zoomable zoomable;

    public Zoomable getZoomable() {
        return zoomable;
    }

    public void setZoomable(Zoomable z) {
        this.zoomable = z;
    }

    private Menu menu;
    private MenuItem twentyFiveItem;
    private MenuItem fiftyItem;
    private MenuItem seventyFiveItem;
    private MenuItem hundredItem;
    private MenuItem hundredFiftyItem;
    private MenuItem twoHundredItem;
    private MenuItem fitItem;

    public Menu getMenu(Control parent) {
        menu = createMenu(parent);
        return menu;
    }

    private Menu createMenu(Control parent) {
        Menu objMenu = new Menu(parent);

        // twenty-five
        twentyFiveItem = new MenuItem(objMenu, SWT.RADIO);
        twentyFiveItem.setData(Integer.valueOf(25));
        twentyFiveItem.setText("25%");
        twentyFiveItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(twentyFiveItem);
            }
        });

        // fifty
        fiftyItem = new MenuItem(objMenu, SWT.RADIO);
        fiftyItem.setData(Integer.valueOf(50));
        fiftyItem.setText("50%");
        fiftyItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(fiftyItem);
            }
        });

        // seventy-five
        seventyFiveItem = new MenuItem(objMenu, SWT.RADIO);
        seventyFiveItem.setData(Integer.valueOf(75));
        seventyFiveItem.setText("75%");
        seventyFiveItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(seventyFiveItem);
            }
        });

        // hundred
        hundredItem = new MenuItem(objMenu, SWT.RADIO);
        hundredItem.setData(Integer.valueOf(100));
        hundredItem.setText("100%");
        hundredItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(hundredItem);
            }
        });

        // hundred fifty
        hundredFiftyItem = new MenuItem(objMenu, SWT.RADIO);
        hundredFiftyItem.setData(Integer.valueOf(150));
        hundredFiftyItem.setText("150%");
        hundredFiftyItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(hundredFiftyItem);
            }
        });

        // two hundred
        twoHundredItem = new MenuItem(objMenu, SWT.RADIO);
        twoHundredItem.setData(Integer.valueOf(200));
        twoHundredItem.setText("200%");
        twoHundredItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(twoHundredItem);
            }
        });

        // fit
        fitItem = new MenuItem(objMenu, SWT.RADIO);
        fitItem.setData(Integer.valueOf(0));
        fitItem.setText("Fit");
        fitItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                select(fitItem);
            }
        });

        int zoomLevel = zoomable.getZoomLevel();

        if (zoomLevel == 25)
            select(twentyFiveItem);
        else if (zoomLevel == 50)
            select(fiftyItem);
        else if (zoomLevel == 75)
            select(seventyFiveItem);
        else if (zoomLevel == 100)
            select(hundredItem);
        else if (zoomLevel == 150)
            select(hundredFiftyItem);
        else if (zoomLevel == 200)
            select(twoHundredItem);
        else if (zoomLevel == 0)
            select(fitItem);

        return objMenu;
    }

    private void select(MenuItem item) {
        twentyFiveItem.setSelection(false);
        fiftyItem.setSelection(false);
        seventyFiveItem.setSelection(false);
        hundredItem.setSelection(false);
        hundredFiftyItem.setSelection(false);
        twoHundredItem.setSelection(false);
        fitItem.setSelection(false);
        item.setSelection(true);

        Integer zoomLevel = (Integer) item.getData();
        zoomable.setZoomLevel(zoomLevel.intValue());
    }

    public Menu getMenu(Menu parent) {
        // not used
        return null;
    }

    public void dispose() {
        if (menu != null)
            menu.dispose();
    }

    public interface Zoomable {
        public int getZoomLevel();

        public void setZoomLevel(int zoomLevel);
    }
}
