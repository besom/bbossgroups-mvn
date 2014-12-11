/*
 * Licensed under the GPL License. You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://probe.jstripe.com/d/license.shtml
 *
 *  THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 *  WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.frameworkset.util.tokenizer;

import java.io.IOException;
import java.io.StringReader;

public class StringTokenizer extends Tokenizer {

    public StringTokenizer() {
    }

    public StringTokenizer(final String str) {
        setString(str);
    }

    public void setString(final String str) {
        setReader(new StringReader(str));
    }

    public boolean hasMore() {
        try {
            return super.hasMore();
        } catch (IOException e) {
            return false;
        }
    }

    public Token getToken() {
        try {
            return super.getToken();
        } catch (IOException e) {
            return null;
        }
    }

    public Token nextToken() {
        try {
            return super.nextToken();
        } catch (IOException e) {
            return null;
        }
    }
}
