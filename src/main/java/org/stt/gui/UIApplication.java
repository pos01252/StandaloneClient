package org.stt.gui;

import dagger.Component;
import net.engio.mbassy.bus.MBassador;
import org.stt.BaseModule;
import org.stt.I18NModule;
import org.stt.command.CommandModule;
import org.stt.config.ConfigModule;
import org.stt.config.ConfigServiceFacade;
import org.stt.event.EventBusModule;
import org.stt.event.ItemLogService;
import org.stt.fun.AchievementModule;
import org.stt.gui.jfx.JFXModule;
import org.stt.gui.jfx.MainWindowController;
import org.stt.persistence.BackupCreator;
import org.stt.persistence.stt.STTPersistenceModule;
import org.stt.text.TextModule;
import org.stt.time.TimeUtilModule;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

@Singleton
@Component(modules = {TimeUtilModule.class, STTPersistenceModule.class, I18NModule.class,
        EventBusModule.class, TextModule.class, JFXModule.class, BaseModule.class, ConfigModule.class,
        AchievementModule.class, CommandModule.class})
public interface UIApplication {
    MBassador<Object> eventBus();

    ConfigServiceFacade configService();

    BackupCreator backupCreator();

    ItemLogService itemLogService();

    MainWindowController mainWindow();

    ExecutorService executorService();
}
