/*
 * GeneralPreferencesPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.GeneralPrefs;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.ProjectsPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 UIPrefs prefs,
                                 final GlobalDisplay globalDisplay,
                                 SourceServerOperations server)
   {
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      prefs_ = prefs;
      server_ = server;

      if (Desktop.isDesktop())
      {
         if (Desktop.getFrame().canChooseRVersion())
         {
            rVersion_ = new TextBoxWithButton(
                  "R version:",
                  "Change...",
                  new ClickHandler()
                  {
                     public void onClick(ClickEvent event)
                     {
                        String ver = Desktop.getFrame().chooseRVersion();
                        if (!StringUtil.isNullOrEmpty(ver))
                        {
                           rVersion_.setText(ver);

                           globalDisplay.showMessage(MessageDialog.INFO,
                                 "Change R Version",
                                 "You need to quit and re-open RStudio " +
                                 "in order for this change to take effect.");
                        }
                     }
                  });
            rVersion_.setWidth("100%");
            rVersion_.setText(Desktop.getFrame().getRVersion());
            spaced(rVersion_);
            add(rVersion_);
         }
      }

      Label defaultLabel = new Label("Default working directory (when not in a project):");
      nudgeRight(defaultLabel);
      add(tight(defaultLabel));
      add(dirChooser_ = new DirectoryChooserTextBox(null, 
                                                    null,
                                                    fileDialogs_, 
                                                    fsContext_));  
      spaced(dirChooser_);
      nudgeRight(dirChooser_);
      textBoxWithChooser(dirChooser_);

      restoreLastProject_ = new CheckBox("Restore most recently opened project at startup");
      lessSpaced(restoreLastProject_);
      add(restoreLastProject_);
      
      add(checkboxPref("Restore previously open source documents at startup", prefs_.restoreSourceDocuments()));
        
      add(loadRData_ = new CheckBox("Restore .RData into workspace at startup"));
      lessSpaced(loadRData_); 
      
      saveWorkspace_ = new SelectWidget(
            "Save workspace to .RData on exit:",
            new String[] {
                  "Always",
                  "Never",
                  "Ask"
            });
      spaced(saveWorkspace_);
      add(saveWorkspace_);
      
      alwaysSaveHistory_ = new CheckBox(
            "Always save history (even when not saving .RData)");
      lessSpaced(alwaysSaveHistory_);
      add(alwaysSaveHistory_);
      
      removeHistoryDuplicates_ = new CheckBox(
                                 "Remove duplicate entries in history");
      spaced(removeHistoryDuplicates_);
      add(removeHistoryDuplicates_);

      rProfileOnResume_ = new CheckBox("Run Rprofile when resuming suspended session");
      spaced(rProfileOnResume_);
      if (!Desktop.isDesktop())
         add(rProfileOnResume_);
           
      encodingValue_ = prefs_.defaultEncoding().getGlobalValue();
      add(encoding_ = new TextBoxWithButton(
            "Default text encoding:",
            "Change...",
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
                  {
                     @Override
                     public void onResponseReceived(IconvListResult response)
                     {
                        new ChooseEncodingDialog(
                              response.getCommon(),
                              response.getAll(),
                              encodingValue_,
                              true,
                              false,
                              new OperationWithInput<String>()
                              {
                                 public void execute(String encoding)
                                 {
                                    if (encoding == null)
                                       return;

                                    setEncoding(encoding);
                                 }
                              }).showModal();
                     }
                  });

               }
            }));
      nudgeRight(encoding_);
      textBoxWithChooser(encoding_);
      setEncoding(prefs.defaultEncoding().getGlobalValue());
            
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
      rProfileOnResume_.setEnabled(false);
      restoreLastProject_.setEnabled(false);
   }
   
   @Override
   protected void initialize(RPrefs rPrefs)
   {
      // general prefs
      GeneralPrefs generalPrefs = rPrefs.getGeneralPrefs();
      
      saveWorkspace_.setEnabled(true);
      loadRData_.setEnabled(true);
      dirChooser_.setEnabled(true);
      
      int saveWorkspaceIndex;
      switch (generalPrefs.getSaveAction())
      {
         case SaveAction.NOSAVE: 
            saveWorkspaceIndex = 1; 
            break;
         case SaveAction.SAVE: 
            saveWorkspaceIndex = 0; 
            break; 
         case SaveAction.SAVEASK:
         default: 
            saveWorkspaceIndex = 2; 
            break; 
      }
      saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

      loadRData_.setValue(generalPrefs.getLoadRData());
      dirChooser_.setText(generalPrefs.getInitialWorkingDirectory());
        
      // history prefs
      HistoryPrefs historyPrefs = rPrefs.getHistoryPrefs();
      
      alwaysSaveHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);
      
      alwaysSaveHistory_.setValue(historyPrefs.getAlwaysSave());
      removeHistoryDuplicates_.setValue(historyPrefs.getRemoveDuplicates());
      
      rProfileOnResume_.setValue(generalPrefs.getRprofileOnResume());
      rProfileOnResume_.setEnabled(true);
     
      // projects prefs
      ProjectsPrefs projectsPrefs = rPrefs.getProjectsPrefs();
      restoreLastProject_.setEnabled(true);
      restoreLastProject_.setValue(projectsPrefs.getRestoreLastProject());
   }
   

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconR();
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);

      prefs_.defaultEncoding().setGlobalValue(encodingValue_);
      
      if (saveWorkspace_.isEnabled())
      {
         int saveAction;
         switch (saveWorkspace_.getListBox().getSelectedIndex())
         {
            case 0: 
               saveAction = SaveAction.SAVE; 
               break; 
            case 1: 
               saveAction = SaveAction.NOSAVE; 
               break; 
            case 2:
            default: 
               saveAction = SaveAction.SAVEASK; 
               break; 
         }

         // set general prefs
         GeneralPrefs generalPrefs = GeneralPrefs.create(saveAction, 
                                                         loadRData_.getValue(),
                                                         rProfileOnResume_.getValue(),
                                                         dirChooser_.getText());
         rPrefs.setGeneralPrefs(generalPrefs);
         
         // set history prefs
         HistoryPrefs historyPrefs = HistoryPrefs.create(
                                          alwaysSaveHistory_.getValue(),
                                          removeHistoryDuplicates_.getValue());
         rPrefs.setHistoryPrefs(historyPrefs);
         
         
         // set projects prefs
         ProjectsPrefs projectsPrefs = ProjectsPrefs.create(
                                             restoreLastProject_.getValue());
         rPrefs.setProjectsPrefs(projectsPrefs);
      }

      return restartRequired;
   }

   @Override
   public String getName()
   {
      return "General";
   }

   private void setEncoding(String encoding)
   {
      encodingValue_ = encoding;
      if (StringUtil.isNullOrEmpty(encoding))
         encoding_.setText(ChooseEncodingDialog.ASK_LABEL);
      else
         encoding_.setText(encoding);
   }


   
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox removeHistoryDuplicates_;
   private CheckBox restoreLastProject_;
   private CheckBox rProfileOnResume_;
   private final SourceServerOperations server_;
   private final UIPrefs prefs_;
   private final TextBoxWithButton encoding_;
   private String encodingValue_;
}
