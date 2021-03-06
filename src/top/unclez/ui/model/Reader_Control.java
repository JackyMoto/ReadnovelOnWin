package top.unclez.ui.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.WindowEvent;
import top.unclez.Utils.FileUtil;
import top.unclez.bean.GlobalValue;
import top.unclez.spider.MainSpider;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Reader_Control implements Initializable {
    @FXML
    Label green;
    @FXML
    Label yellow;
    @FXML
    Label dark;
    @FXML
    Label gray;
    @FXML
    Label graydark;
    @FXML
    Label white;
    @FXML
    Label defaults;
    @FXML
    Label add;
    @FXML
    Label less;
    @FXML
    Button setting;
    @FXML
    Button chapterbtn;
    @FXML
    ListView<String> chapter;
    @FXML
    TextArea contents;
    String text = "";
    int index = 0;
    long changedTime = new Date().getTime();
    long changed = 0;
    String userColor = "#CEEBCE";
    String userfontColor = "#333333";
    String[] chacheContent = new String[3];
    ObservableList<String> chapterdata;
    boolean isChached = true;

    public void initialize(URL location, ResourceBundle resources) {
        init();//加载配置，目录
        showset();//隐藏设置
        //关闭时自动保存进度
        GlobalValue.readstage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                String path = "bookRead/" + GlobalValue.config.getNovelname();
                try (PrintWriter out = new PrintWriter(new FileOutputStream(new File(path + ".index")));
                     PrintWriter outuser = new PrintWriter(new FileOutputStream(new File(path + ".config")))) {
                    //保存书籍目录
                    for (String s : GlobalValue.taskurl) {
                        out.println(s + "---" + GlobalValue.data.get(s));
                    }
                    //保存书籍阅读配置和阅读位置
                    outuser.println("index:" + index);
                    outuser.println("charset:" + GlobalValue.config.getCharset());
                    outuser.println("color:" + userColor);
                    outuser.println("userfontcolor:" + userfontColor);
                    outuser.println("fontsize:" + (int) (contents.getFont().getSize()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                GlobalValue.readstage = null;
                GlobalValue.stage.show();
            }
        });
        //初始化正文内容
        chapterdata = FXCollections.observableArrayList();
        for (String c : GlobalValue.taskurl) {
            chapterdata.add(GlobalValue.data.get(c));
        }
        String url = GlobalValue.taskurl.get(index);
        setContent(url);
        //正文滚动条事件监控，到顶自动加载上一章，到底加载下一章节，至少间隔5秒响应一次
        contents.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                changed = new Date().getTime();
                System.out.println(event.getDeltaY() + "-" + event.getMultiplierY() + "--" + event.getTextDeltaY() + "--" + event.getTouchCount());
                System.out.println(Math.abs(changed - changedTime));
                if ((Math.abs(changed - changedTime)) > 5000 && isChached) {
                    if (event.getDeltaY() == 40.0) {
                        if (index == 0) {
                            return;
                        }
                        GlobalValue.readstage.setTitle(GlobalValue.data.get(GlobalValue.taskurl.get(index - 1)));
                        setContent("上一页");
                    } else {
                        GlobalValue.readstage.setTitle(GlobalValue.data.get(GlobalValue.taskurl.get(index + 1)));
                        setContent("下一页");
                    }
                    changedTime = changed;
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (userColor != null) {
                            changeColor(userColor, userfontColor);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 初始化设置
     *
     * @param event
     */
    public void onCreate(ActionEvent event) {
        setting.setOnMouseClicked(e -> {
            switch (setting.getText()) {
                case "设置":
                    setting.setText("隐藏");
                    break;
                case "隐藏":
                    setting.setText("设置");
                    break;
            }
            showset();
        });
        yellow.setOnMouseClicked(e -> {
            changeColor("#e6dbbf", "#333333");
        });
        gray.setOnMouseClicked(e -> {
            changeColor("#AAAAAA", "#333333");
        });
        graydark.setOnMouseClicked(e -> {
            changeColor("#bfbfbf", "#333333");
        });
        green.setOnMouseClicked(e -> {
            changeColor("#bed0cf", "#333333");
        });
        white.setOnMouseClicked(e -> {
            changeColor("#ffffff", "#333333");
        });
        defaults.setOnMouseClicked(e -> {
            changeColor("#CEEBCE", "#333333");
        });
        dark.setOnMouseClicked(e -> {
            changeColor("#3A3131", "#95938F");
        });
        add.setOnMouseClicked(e -> {
            contents.setFont(new Font(contents.getFont().getSize() + 1));
        });
        less.setOnMouseClicked(e -> {
            contents.setFont(new Font(contents.getFont().getSize() - 1));
        });
    }

    /**
     * 显示目录，设置双击显示正文
     *
     * @param event
     */
    public void showChapter(ActionEvent event) {
        chapterbtn.setOnMouseClicked(e -> {
            chapter.setVisible(!chapter.isVisible());
            chapter.setItems(chapterdata);
            chapter.scrollTo(index);
            chapter.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                        index = chapter.getSelectionModel().getSelectedIndex();
                        String url = GlobalValue.taskurl.get(index);
                        chacheContent = new String[3];//清除缓存
                        setContent(url);
                        GlobalValue.readstage.setTitle(GlobalValue.data.get(url));
                    }
                }
            });
        });
    }

    /**
     * 切换背景
     *
     * @param color     背景色
     * @param fontcolor 字体颜色
     */
    public void changeColor(String color, String fontcolor) {
        contents.setStyle("-fx-text-fill: " + fontcolor + ";");
        Region region = (Region) contents.lookup(".content");
//        region.setBackground(new Background(new BackgroundFill(Color.BROWN, CornerRadii.EMPTY, Insets.EMPTY)));
        region.setStyle("-fx-background-color: " + color + ";");
        userColor = color;
        userfontColor = fontcolor;
    }

    //初始化配置
    public void init() {
        //加载目录(加载了目录，taskurl，dada，path)
        FileUtil.openBook();
        //加载配置（加载颜色，字体颜色，字体大小，编码）
        Map<String, String> conf = FileUtil.openConfig();
        if (conf == null)
            return;
        GlobalValue.config.setCharset(conf.get("charset"));
        index = Integer.parseInt(conf.get("index"));
        userColor = conf.get("color");
        userfontColor = conf.get("userfontcolor");
        contents.setFont(new Font(Integer.parseInt(conf.get("fontsize"))));
    }

    //隐藏设置打开设置
    private void showset() {
        yellow.setVisible(!yellow.isVisible());
        green.setVisible(!green.isVisible());
        graydark.setVisible(!graydark.isVisible());
        gray.setVisible(!gray.isVisible());
        dark.setVisible(!dark.isVisible());
        defaults.setVisible(!defaults.isVisible());
        white.setVisible(!white.isVisible());
        add.setVisible(!add.isVisible());
        less.setVisible(!less.isVisible());
    }

    /**
     * 加载正文内容，缓存存在则加载缓存，不存在则爬取
     *
     * @param url 要爬取的章节url
     */
    private void setContent(String url) {
        if (url.startsWith("http"))
            GlobalValue.readstage.setTitle(GlobalValue.data.get(url) + "    加载中。。");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (url.startsWith("http")) {
                    text = MainSpider.getContent(url, GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                    chacheContent[1] = text;
                } else if (url.equals("上一页")) {
                    text = chacheContent[0];
                    --index;
                } else {
                    text = chacheContent[2];
                    ++index;
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (url.startsWith("http"))
                            GlobalValue.readstage.setTitle(GlobalValue.data.get(url));
                        contents.setText(text);
                        chapter.setVisible(false);
                    }
                });
                chacheText(index, url.equals("上一页"));//缓存内容
            }
        }).start();
    }

    /**
     * 缓存正文内容，上一章节，当前章节，下一章节
     *
     * @param index 当前章节位置
     */
    private void chacheText(int index, boolean isup) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isChached = false;//缓存开始，禁止翻页
                //第一章节时
                if (index == 0) {
                    chacheContent[2] = MainSpider.getContent(GlobalValue.taskurl.get(index + 1), GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                } else if (chacheContent[0] == null && index != 0) {//书架打开时，不是第一章，但是又没有缓存可以重用
                    chacheContent[0] = MainSpider.getContent(GlobalValue.taskurl.get(index - 1), GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                    chacheContent[2] = MainSpider.getContent(GlobalValue.taskurl.get(index + 1), GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                } else if (isup) {
                    chacheContent[2] = chacheContent[1];
                    chacheContent[1] = chacheContent[0];
                    chacheContent[0] = MainSpider.getContent(GlobalValue.taskurl.get(index - 1), GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                } else {
                    chacheContent[0] = chacheContent[1];
                    chacheContent[1] = chacheContent[2];
                    chacheContent[2] = MainSpider.getContent(GlobalValue.taskurl.get(index + 1), GlobalValue.config.getCharset(), GlobalValue.config.getRule());
                }
                isChached = true;//缓存结束，可以翻页
            }
        }).start();
    }
}
