/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.utils;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.tcf.core.concurrent.Rendezvous;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.filesystem.core.services.Operation;

/**
 * A blocking call proxy for a file system service. All calls to the service method are blocked
 * until its "Done" handler is invoked.
 * <p>
 * <em>Note that all method call over the proxy must be made <b>OUTSIDE</b> of
 * the dispatching thread.</em> If it is called inside of the dispatching thread, the call will be
 * blocked forever.
 * <p>
 * This class is used to replace BlockingProxyCall for better debugability.
 *
 */
public class BlockingFileSystemProxy implements IFileSystem {
	// The default timeout waiting for blocked invocations.
	private static final long DEFAULT_TIMEOUT = Operation.DEFAULT_TIMEOUT;
	// The actual object that provides file system services.
	IFileSystem service;

	/**
	 * Constructor with an delegating service.
	 *
	 * @param service The delegating service.
	 */
	public BlockingFileSystemProxy(IFileSystem service) {
		Assert.isNotNull(service);
		this.service = service;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.protocol.IService#getName()
	 */
	@Override
	public String getName() {
		return service.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#open(java.lang.String, int, org.eclipse.tcf.services.IFileSystem.FileAttrs, org.eclipse.tcf.services.IFileSystem.DoneOpen)
	 */
	@Override
	public IToken open(final String file_name, final int flags, final FileAttrs attrs, final DoneOpen done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.open(file_name, flags, attrs, new DoneOpen() {
					@Override
					public void doneOpen(final IToken token, final FileSystemException error, final IFileHandle handle) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneOpen(token, error, handle);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutOpeningFile, file_name));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#close(org.eclipse.tcf.services.IFileSystem.IFileHandle, org.eclipse.tcf.services.IFileSystem.DoneClose)
	 */
	@Override
	public IToken close(final IFileHandle handle, final DoneClose done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.close(handle, new DoneClose() {
					@Override
					public void doneClose(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneClose(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutClosingFile, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#read(org.eclipse.tcf.services.IFileSystem.IFileHandle, long, int, org.eclipse.tcf.services.IFileSystem.DoneRead)
	 */
	@Override
	public IToken read(final IFileHandle handle, final long offset, final int len, final DoneRead done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.read(handle, offset, len, new DoneRead() {
					@Override
					public void doneRead(final IToken token, final FileSystemException error, final byte[] data, final boolean eof) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRead(token, error, data, eof);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutReadingFile, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#write(org.eclipse.tcf.services.IFileSystem.IFileHandle, long, byte[], int, int, org.eclipse.tcf.services.IFileSystem.DoneWrite)
	 */
	@Override
	public IToken write(final IFileHandle handle, final long offset, final byte[] data, final int data_pos, final int data_size, final DoneWrite done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.write(handle, offset, data, data_pos, data_size, new DoneWrite() {
					@Override
					public void doneWrite(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneWrite(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutWritingFile, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#stat(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneStat)
	 */
	@Override
	public IToken stat(final String path, final DoneStat done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.stat(path, new DoneStat() {
					@Override
					public void doneStat(final IToken token, final FileSystemException error, final FileAttrs attrs) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneStat(token, error, attrs);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutStat, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#lstat(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneStat)
	 */
	@Override
	public IToken lstat(final String path, final DoneStat done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.lstat(path, new DoneStat() {
					@Override
					public void doneStat(final IToken token, final FileSystemException error, final FileAttrs attrs) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneStat(token, error, attrs);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutLstat, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#fstat(org.eclipse.tcf.services.IFileSystem.IFileHandle, org.eclipse.tcf.services.IFileSystem.DoneStat)
	 */
	@Override
	public IToken fstat(final IFileHandle handle, final DoneStat done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.fstat(handle, new DoneStat() {
					@Override
					public void doneStat(final IToken token, final FileSystemException error, final FileAttrs attrs) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneStat(token, error, attrs);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutFstat, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#setstat(java.lang.String, org.eclipse.tcf.services.IFileSystem.FileAttrs, org.eclipse.tcf.services.IFileSystem.DoneSetStat)
	 */
	@Override
	public IToken setstat(final String path, final FileAttrs attrs, final DoneSetStat done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.setstat(path, attrs, new DoneSetStat() {
					@Override
					public void doneSetStat(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneSetStat(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutSetStat, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#fsetstat(org.eclipse.tcf.services.IFileSystem.IFileHandle, org.eclipse.tcf.services.IFileSystem.FileAttrs, org.eclipse.tcf.services.IFileSystem.DoneSetStat)
	 */
	@Override
	public IToken fsetstat(final IFileHandle handle, final FileAttrs attrs, final DoneSetStat done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.fsetstat(handle, attrs, new DoneSetStat() {
					@Override
					public void doneSetStat(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneSetStat(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutFSetStat, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#opendir(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneOpen)
	 */
	@Override
	public IToken opendir(final String path, final DoneOpen done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.opendir(path, new DoneOpen() {
					@Override
					public void doneOpen(final IToken token, final FileSystemException error, final IFileHandle handle) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneOpen(token, error, handle);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutOpeningDir, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#readdir(org.eclipse.tcf.services.IFileSystem.IFileHandle, org.eclipse.tcf.services.IFileSystem.DoneReadDir)
	 */
	@Override
	public IToken readdir(final IFileHandle handle, final DoneReadDir done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.readdir(handle, new DoneReadDir() {
					@Override
					public void doneReadDir(final IToken token, final FileSystemException error, final DirEntry[] entries, final boolean eof) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneReadDir(token, error, entries, eof);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutReadingDir, handle));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#mkdir(java.lang.String, org.eclipse.tcf.services.IFileSystem.FileAttrs, org.eclipse.tcf.services.IFileSystem.DoneMkDir)
	 */
	@Override
	public IToken mkdir(final String path, final FileAttrs attrs, final DoneMkDir done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.mkdir(path, attrs, new DoneMkDir() {
					@Override
					public void doneMkDir(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneMkDir(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutMakingDir, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#rmdir(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneRemove)
	 */
	@Override
	public IToken rmdir(final String path, final DoneRemove done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.rmdir(path, new DoneRemove() {
					@Override
					public void doneRemove(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRemove(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutRemovingDir, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#roots(org.eclipse.tcf.services.IFileSystem.DoneRoots)
	 */
	@Override
	public IToken roots(final DoneRoots done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.roots(new DoneRoots() {
					@Override
					public void doneRoots(final IToken token, final FileSystemException error, final DirEntry[] entries) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRoots(token, error, entries);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(Messages.BlockingFileSystemProxy_TimeoutListingRoots);
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#remove(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneRemove)
	 */
	@Override
	public IToken remove(final String file_name, final DoneRemove done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.remove(file_name, new DoneRemove() {
					@Override
					public void doneRemove(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRemove(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutRemovingFile, file_name));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#realpath(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneRealPath)
	 */
	@Override
	public IToken realpath(final String path, final DoneRealPath done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.realpath(path, new DoneRealPath() {
					@Override
					public void doneRealPath(final IToken token, final FileSystemException error, final String path) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRealPath(token, error, path);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutGettingRealPath, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#rename(java.lang.String, java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneRename)
	 */
	@Override
	public IToken rename(final String old_path, final String new_path, final DoneRename done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.rename(old_path, new_path, new DoneRename() {
					@Override
					public void doneRename(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneRename(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutRenamingFile, old_path, new_path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#readlink(java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneReadLink)
	 */
	@Override
	public IToken readlink(final String path, final DoneReadLink done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.readlink(path, new DoneReadLink() {
					@Override
					public void doneReadLink(final IToken token, final FileSystemException error, final String path) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneReadLink(token, error, path);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutReadingLink, path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#symlink(java.lang.String, java.lang.String, org.eclipse.tcf.services.IFileSystem.DoneSymLink)
	 */
	@Override
	public IToken symlink(final String link_path, final String target_path, final DoneSymLink done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.symlink(link_path, target_path, new DoneSymLink() {
					@Override
					public void doneSymLink(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneSymLink(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutSymLink, link_path, target_path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#copy(java.lang.String, java.lang.String, boolean, boolean, org.eclipse.tcf.services.IFileSystem.DoneCopy)
	 */
	@Override
	public IToken copy(final String src_path, final String dst_path, final boolean copy_permissions, final boolean copy_ownership, final DoneCopy done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.copy(src_path, dst_path, copy_permissions, copy_ownership, new DoneCopy() {
					@Override
					public void doneCopy(final IToken token, final FileSystemException error) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneCopy(token, error);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(NLS.bind(Messages.BlockingFileSystemProxy_TimeoutCopying, src_path, dst_path));
		}
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IFileSystem#user(org.eclipse.tcf.services.IFileSystem.DoneUser)
	 */
	@Override
	public IToken user(final DoneUser done) {
		Assert.isTrue(!Protocol.isDispatchThread());
		final Rendezvous rendezvous = new Rendezvous();
		final AtomicReference<IToken> ref = new AtomicReference<IToken>();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				IToken token = service.user(new DoneUser() {
					@Override
					public void doneUser(final IToken token, final FileSystemException error, final int real_uid, final int effective_uid, final int real_gid, final int effective_gid, final String home) {
						SafeRunner.run(new ISafeRunnable() {
							@Override
							public void handleException(Throwable exception) {
							}

							@Override
							public void run() throws Exception {
								done.doneUser(token, error, real_uid, effective_uid, real_gid, effective_gid, home);
							}
						});
						rendezvous.arrive();
					}
				});
				ref.set(token);
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch (TimeoutException e) {
			throw new RuntimeException(Messages.BlockingFileSystemProxy_TimeoutGettingUser);
		}
		return ref.get();
	}
}
