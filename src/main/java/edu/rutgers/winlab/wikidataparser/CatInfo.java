package edu.rutgers.winlab.wikidataparser;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class CatInfo {

    private final int id;
    private final String name;
    private final HashSet<CatInfo> children = new HashSet<>(), parents = new HashSet<>();
    private int pageCount, fileCount, subCatCount;

    public CatInfo(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int incrementPageCount() {
        return ++pageCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int incrementFileCount() {
        return ++fileCount;
    }

    public int getSubCatCount() {
        return subCatCount;
    }

    public void setSubCatCount(int subCatCount) {
        this.subCatCount = subCatCount;
    }

    public int incrementSubCatCount() {
        return ++subCatCount;
    }

    public void addChild(CatInfo cat) {
        children.add(cat);
        cat.parents.add(this);
    }

    public void forEachChild(Consumer<CatInfo> c) {
        children.forEach(c);
    }

    public Stream<CatInfo> getChildrenStream() {
        return children.stream();
    }
    
    public int getChildrenCount() {
        return children.size();
    }

    public void forEachParent(Consumer<CatInfo> c) {
        parents.forEach(c);
    }

    public Stream<CatInfo> getParentsStream() {
        return parents.stream();
    }
    
    public int getParentsCount() {
        return parents.size();
    }

    public boolean isOrphan() {
        return parents.isEmpty();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CatInfo other = (CatInfo) obj;
        return this.id == other.id;
    }
}
