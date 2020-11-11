package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.util.LruCache
import jcifs.CIFSContext
import jcifs.smb.SmbFile

/**
 * CIFS Context cache
 */
class CifsContextCache: LruCache<CifsConnection, CIFSContext>(10)

/**
 * SMB File cache
 */
class SmbFileCache: LruCache<String, SmbFile>(100)
