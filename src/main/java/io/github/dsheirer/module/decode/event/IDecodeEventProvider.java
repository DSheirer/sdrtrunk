package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.sample.Listener;

public interface IDecodeEventProvider
{
	void addDecodeEventListener(Listener<IDecodeEvent> listener );
	void removeDecodeEventListener(Listener<IDecodeEvent> listener );
}
