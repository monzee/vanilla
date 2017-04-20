package ph.codeia.androidutils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ph.codeia.query.Params;
import ph.codeia.query.Queryable;
import ph.codeia.query.Results;
import ph.codeia.query.Row;

/**
 * This file is a part of the vanilla project.
 */

public class AndroidContent implements Queryable {

    private final ContentResolver resolver;
    private final Uri baseUri;

    public AndroidContent(ContentResolver resolver, Uri baseUri) {
        this.resolver = resolver;
        this.baseUri = baseUri;
    }

    @Override
    public <T> Results<T> run(Params in, Row.Mapper<T> out) {
        return get(
                in.dataset(), in.projection(), in.selection(),
                in.selectionArgs(), in.sortOrder(), out);
    }

    public <T> Results<T> get(
            String path,
            String[] projection,
            String selection,
            String[] args,
            String ordering,
            final Row.Mapper<T> factory) {
        final Cursor cursor = resolver.query(
                path != null && !path.isEmpty()
                        ? Uri.withAppendedPath(baseUri, path)
                        : baseUri,
                projection,
                selection,
                args,
                ordering);
        return new Results<T>() {
            @Override
            public boolean ok() {
                return cursor != null;
            }

            @Override
            public int count() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public void dispose() {
                if (cursor != null) {
                    cursor.close();
                }
            }

            @Override
            public Iterator<T> iterator() {
                if (cursor == null) {
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public T next() {
                            throw new NoSuchElementException();
                        }
                    };
                }
                cursor.moveToFirst();
                return new Iterator<T>() {
                    final Row row = new Row() {
                        @Override
                        public int getInt(int column) {
                            return cursor.getInt(column);
                        }

                        @Override
                        public long getLong(int column) {
                            return cursor.getLong(column);
                        }

                        @Override
                        public short getShort(int column) {
                            return cursor.getShort(column);
                        }

                        @Override
                        public float getFloat(int column) {
                            return cursor.getFloat(column);
                        }

                        @Override
                        public double getDouble(int column) {
                            return cursor.getDouble(column);
                        }

                        @Override
                        public String getString(int column) {
                            return cursor.getString(column);
                        }

                        @Override
                        public byte[] getBlob(int column) {
                            return cursor.getBlob(column);
                        }
                    };

                    @Override
                    public boolean hasNext() {
                        return !cursor.isAfterLast();
                    }

                    @Override
                    public T next() {
                        T result = factory.from(row);
                        cursor.moveToNext();
                        return result;
                    }
                };
            }

            @Override
            protected void finalize() throws Throwable {
                dispose();
                super.finalize();
            }
        };
    }
}
