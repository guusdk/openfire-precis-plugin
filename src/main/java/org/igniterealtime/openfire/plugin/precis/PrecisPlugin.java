/*
 * Copyright (C) 2022 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.openfire.plugin.precis;

import org.jivesoftware.database.*;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocks.xmpp.precis.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.xmpp.packet.*;

public class PrecisPlugin implements Plugin
{
    private static final Logger Log = LoggerFactory.getLogger( PrecisPlugin.class );


    @Override
    public void initializePlugin( final PluginManager manager, final File pluginDirectory )
    {
        Log.info("Start.");
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement("SELECT username FROM ofUser");
            rs = pstmt.executeQuery();
            final Set<String> usernames = new HashSet<>();
            final Set<String> invalids = new HashSet<>();
            final Set<String> duplicates = new HashSet<>();
            final Set<String> jidusernames = new HashSet<>();
            final Set<String> jidinvalids = new HashSet<>();
            final Set<String> jidduplicates = new HashSet<>();
            while (rs.next()) {
                final String username = rs.getString(1);
                try {
                    final String comparable = PrecisProfiles.USERNAME_CASE_MAPPED.toComparableString(username);
                    if (!usernames.add(comparable)) {
                        duplicates.add(comparable);
                    }
                } catch (InvalidCodePointException | InvalidDirectionalityException e) {
                    invalids.add(username);
                }
                try {
                    final String comparable = JID.nodeprep(username);
                    if (!jidusernames.add(comparable)) {
                        jidduplicates.add(comparable);
                    }
                } catch (IllegalArgumentException e) {
                    jidinvalids.add(username);
                }
            }
            Log.info("PRECIS: Found {} unique usernames, {} duplicates", usernames.size(), duplicates.size());
            Log.info("PRECIS: Found {} unique invalids.", invalids.size());
            Log.info("JIDJID: Found {} unique usernames, {} duplicates", jidusernames.size(), jidduplicates.size());
            Log.info("JIDJID: Found {} unique invalids.", jidinvalids.size());
            invalids.forEach(i -> Log.info("PRECIS invalid: [{}]", i));
        } catch (Throwable t) {
            Log.info("Oops!", t);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        Log.info("Done.");
    }

    @Override
    public void destroyPlugin()
    {
    }
}
