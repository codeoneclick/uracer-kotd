package com.bitfire.uracer.game.logic.messager;

import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.game.logic.messager.Message.Position;
import com.bitfire.uracer.game.logic.messager.Message.Size;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Type;

public class Messager extends GameTask {
	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			for( Position group : Position.values() ) {
				if( isBusy( group ) ) {
					currents.get( group.ordinal() ).render( batch );
				}
			}
		}
	};

	// data
	private Array<LinkedList<Message>> messages;
	private Array<Message> currents;
	private Message[] messageStore;
	private static final int MaxMessagesInStore = 10;
	private int idxMessageStore;

	public Messager( float invZoomFactor ) {
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.MINUS_4 );

		currents = new Array<Message>( 3 );
		for( Position group : Position.values() ) {
			currents.insert( group.ordinal(), null );
		}

		messages = new Array<LinkedList<Message>>( 3 );
		for( Position group : Position.values() ) {
			messages.insert( group.ordinal(), new LinkedList<Message>() );
		}

		// initialize message store
		idxMessageStore = 0;
		messageStore = new Message[ MaxMessagesInStore ];
		for( int i = 0; i < MaxMessagesInStore; i++ ) {
			messageStore[i] = new Message( invZoomFactor );
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		GameEvents.gameRenderer.removeListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.MINUS_4 );
		onReset();
	}

	@Override
	protected void onTick() {
		update( Position.Top );
		update( Position.Middle );
		update( Position.Bottom );
	}

	public boolean isBusy( Position group ) {
		return (currents.get( group.ordinal() ) != null);
	}

	@Override
	public void onReset() {
		for( Position group : Position.values() ) {
			messages.get( group.ordinal() ).clear();
		}

		for( Position group : Position.values() ) {
			currents.set( group.ordinal(), null );
		}

		idxMessageStore = 0;
	}

	private void update( Position group ) {
		LinkedList<Message> msgs = messages.get( group.ordinal() );
		Message msg = currents.get( group.ordinal() );

		// any message?
		if( msg == null && (msgs.peek() != null) ) {
			// schedule next message to process
			msg = msgs.remove();
			currents.set( group.ordinal(), msg );
		}

		// busy or became busy?
		if( msg != null ) {
			// start message if needed
			if( !msg.started ) {
				msg.started = true;
				msg.startMs = System.currentTimeMillis();
				msg.onShow();
			}

			if( !msg.tick() ) {
				currents.set( group.ordinal(), null );
				return;
			}

			// check if finished
			if( (System.currentTimeMillis() - msg.startMs) >= msg.durationMs && !msg.isHiding() ) {
				// message should end
				msg.onHide();
			}
		}
	}

	public void show( String message, float durationSecs, Message.Type type, Position position, Size size ) {
		if( isBusy( position ) ) {
			currents.get( position.ordinal() ).onHide();
		}

		enqueue( message, durationSecs, type, position, size );
	}

	public void enqueue( String message, float durationSecs, Message.Type type, Position position, Size size ) {
		Message m = nextFreeMessage();
		m.set( message, durationSecs, type, position, size );
		messages.get( position.ordinal() ).add( m );
	}

	private Message nextFreeMessage() {
		Message ret = messageStore[idxMessageStore++];
		if( idxMessageStore == MaxMessagesInStore ) {
			idxMessageStore = 0;
		}

		return ret;
	}
}