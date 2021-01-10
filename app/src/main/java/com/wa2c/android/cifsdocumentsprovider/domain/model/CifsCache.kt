package com.wa2c.android.cifsdocumentsprovider.domain.model

import android.util.LruCache
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import java.io.Serializable

/**
 * CIFS Context cache
 */
class CifsContextCache: LruCache<CifsConnection, CIFSContext>(10)

/**
 * CIFS File cache
 */
class CifsFileCache: LruCache<Serializable, CifsFile>(100)

/**
 * SMB File cache
 */
class SmbFileCache: LruCache<Serializable, SmbFile>(100)
