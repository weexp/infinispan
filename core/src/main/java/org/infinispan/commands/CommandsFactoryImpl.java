/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.commands;

import org.infinispan.Cache;
import org.infinispan.commands.control.StateTransferControlCommand;
import org.infinispan.commands.read.GetKeyValueCommand;
import org.infinispan.commands.read.SizeCommand;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commands.remote.MultipleRpcCommand;
import org.infinispan.commands.remote.SingleRpcCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.tx.RollbackCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.EvictCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.InvalidateL1Command;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.container.DataContainer;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.InterceptorChain;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.GlobalTransaction;

import java.util.List;
import java.util.Map;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class CommandsFactoryImpl implements CommandsFactory {
   private DataContainer dataContainer;
   private CacheNotifier notifier;
   private Cache cache;
   private CacheLoaderManager cacheLoaderManager;

   // some stateless commands can be reused so that they aren't constructed again all the time.
   SizeCommand cachedSizeCommand;
   private InterceptorChain interceptorChain;
   private DistributionManager distributionManager;

   @Inject
   public void setupDependencies(DataContainer container, CacheNotifier notifier, Cache cache,
                                 InterceptorChain interceptorChain, CacheLoaderManager clManager,
                                 DistributionManager distributionManager) {
      this.dataContainer = container;
      this.notifier = notifier;
      this.cache = cache;
      this.interceptorChain = interceptorChain;
      this.cacheLoaderManager = clManager;
      this.distributionManager = distributionManager;
   }

   public PutKeyValueCommand buildPutKeyValueCommand(Object key, Object value, long lifespanMillis, long maxIdleTimeMillis) {
      return new PutKeyValueCommand(key, value, false, notifier, lifespanMillis, maxIdleTimeMillis);
   }

   public RemoveCommand buildRemoveCommand(Object key, Object value) {
      return new RemoveCommand(key, value, notifier);
   }

   public InvalidateCommand buildInvalidateCommand(Object... keys) {
      return new InvalidateCommand(notifier, keys);
   }

   public InvalidateCommand buildInvalidateFromL1Command(Object... keys) {
      return new InvalidateL1Command(distributionManager, notifier, keys);
   }

   public ReplaceCommand buildReplaceCommand(Object key, Object oldValue, Object newValue, long lifespan, long maxIdleTimeMillis) {
      return new ReplaceCommand(key, oldValue, newValue, lifespan, maxIdleTimeMillis);
   }

   public SizeCommand buildSizeCommand() {
      if (cachedSizeCommand == null) {
         cachedSizeCommand = new SizeCommand(dataContainer);
      }
      return cachedSizeCommand;
   }

   public GetKeyValueCommand buildGetKeyValueCommand(Object key) {
      return new GetKeyValueCommand(key, notifier);
   }

   public PutMapCommand buildPutMapCommand(Map map, long lifespan, long maxIdleTimeMillis) {
      return new PutMapCommand(map, notifier, lifespan, maxIdleTimeMillis);
   }

   public ClearCommand buildClearCommand() {
      return new ClearCommand(notifier);
   }

   public EvictCommand buildEvictCommand(Object key) {
      EvictCommand command = new EvictCommand(key, notifier);
      return command;
   }

   public PrepareCommand buildPrepareCommand(GlobalTransaction gtx, List modifications, Address localAddress, boolean onePhaseCommit) {
      return new PrepareCommand(gtx, modifications, localAddress, onePhaseCommit);
   }

   public CommitCommand buildCommitCommand(GlobalTransaction gtx) {
      return new CommitCommand(gtx);
   }

   public RollbackCommand buildRollbackCommand(GlobalTransaction gtx) {
      return new RollbackCommand(gtx);
   }

   public MultipleRpcCommand buildReplicateCommand(List<ReplicableCommand> toReplicate) {
      return new MultipleRpcCommand(toReplicate, cache.getName());
   }

   public SingleRpcCommand buildSingleRpcCommand(ReplicableCommand call) {
      return new SingleRpcCommand(cache.getName(), call);
   }

   public StateTransferControlCommand buildStateTransferControlCommand(boolean block) {
      return new StateTransferControlCommand(block);
   }

   public ClusteredGetCommand buildClusteredGetCommand(Object key) {
      return new ClusteredGetCommand(key, cache.getName());
   }

   public void initializeReplicableCommand(ReplicableCommand c) {
      if (c == null) return;
      switch (c.getCommandId()) {
         case PutKeyValueCommand.COMMAND_ID:
            ((PutKeyValueCommand) c).init(notifier);
            break;
         case PutMapCommand.COMMAND_ID:
            ((PutMapCommand) c).init(notifier);
            break;
         case RemoveCommand.COMMAND_ID:
            ((RemoveCommand) c).init(notifier);
            break;
         case MultipleRpcCommand.COMMAND_ID:
            MultipleRpcCommand rc = (MultipleRpcCommand) c;
            rc.setInterceptorChain(interceptorChain);
            if (rc.getCommands() != null)
               for (ReplicableCommand nested : rc.getCommands()) {
                  initializeReplicableCommand(nested);
               }
            break;
         case SingleRpcCommand.COMMAND_ID:
            SingleRpcCommand src = (SingleRpcCommand) c;
            src.setInterceptorChain(interceptorChain);
            if (src.getCommand() != null)
               initializeReplicableCommand(src.getCommand());

            break;
         case InvalidateCommand.COMMAND_ID:
            InvalidateCommand ic = (InvalidateCommand) c;
            ic.init(notifier);
            break;
         case InvalidateL1Command.COMMAND_ID:
            InvalidateL1Command ilc = (InvalidateL1Command) c;
            ilc.init(distributionManager, notifier);
            break;
         case PrepareCommand.COMMAND_ID:
            PrepareCommand pc = (PrepareCommand) c;
            if (pc.getModifications() != null)
               for (ReplicableCommand nested : pc.getModifications()) initializeReplicableCommand(nested);
            break;
         case ClearCommand.COMMAND_ID:
            ClearCommand cc = (ClearCommand) c;
            cc.init(notifier);
            break;
         case ClusteredGetCommand.COMMAND_ID:
            ClusteredGetCommand clusteredGetCommand = (ClusteredGetCommand) c;
            clusteredGetCommand.initialize(dataContainer, cacheLoaderManager);
            break;
      }
   }
}
