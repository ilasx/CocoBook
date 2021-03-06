package com.thmub.cocobook.utils;

import java.io.File;
import java.util.List;

/**
 * Created by zhouas666 on 18-2-28.
 * 文件栈
 */

public class FileStack {

    private Node node = null;
    private int count = 0;

    public void push(FileSnapshot fileSnapshot){
        if (fileSnapshot == null) return;
        Node fileNode = new Node();
        fileNode.fileSnapshot = fileSnapshot;
        fileNode.next = node;
        node = fileNode;
        ++count;
    }

    public FileSnapshot pop(){
        Node fileNode = node;
        if (fileNode == null) return null;
        FileSnapshot fileSnapshot = fileNode.fileSnapshot;
        node = fileNode.next;
        --count;
        return fileSnapshot;
    }

    public int getSize(){
        return count;
    }

    //节点
    public class Node {
        FileSnapshot fileSnapshot;
        Node next;
    }

    //文件快照
    public static class FileSnapshot{
        public String filePath;
        public List<File> files;
        public int scrollOffset;
    }
}
