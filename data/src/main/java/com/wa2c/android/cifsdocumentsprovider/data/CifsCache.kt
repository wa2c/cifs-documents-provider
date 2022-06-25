package com.wa2c.android.cifsdocumentsprovider.data

import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsConnection
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import java.io.Serializable

/**
 * CIFS Context cache
 */
internal class CifsContextCache: LruCache<CifsConnection, CIFSContext>(10)

/**
 * CIFS File cache
 */
internal class CifsFileCache: LruCache<Serializable, CifsFile>(100)

/**
 * SMB File cache
 */
internal class SmbFileCache: LruCache<Serializable, SmbFile>(100)
