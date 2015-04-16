/*
 * Copyright (c) 2014, 2015 Data4All
 * 
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.data4all.util.upload;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.mime.MultipartEntity;

/**
 * A custom MultipartEntity which encapsulates the {@link OutputStream} with a
 * {@link CallbackOutputStream} to provide a callback for the progress of
 * reading from its String content.
 * 
 * @author tbrose, fkirchge
 */
public final class MultiCallbackEntry extends MultipartEntity {
    
    private Callback<Integer> callback;

    /**
     * Constructs a StringEntity with the content {@code content} and the
     * callback {@code callback}.
     * 
     * @param callback
     *            The callback to call
     */
    public MultiCallbackEntry(Callback<Integer> callback) {
        this.callback = callback;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.http.entity.StringEntity#writeTo(java.io.OutputStream)
     */
    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        super.writeTo(new CallbackOutputStream(outstream, callback));
    }
}