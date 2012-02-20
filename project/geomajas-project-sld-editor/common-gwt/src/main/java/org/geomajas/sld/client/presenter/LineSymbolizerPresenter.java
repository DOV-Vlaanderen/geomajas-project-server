package org.geomajas.sld.client.presenter;

import java.util.logging.Logger;

import org.geomajas.sld.LineSymbolizerInfo;
import org.geomajas.sld.client.model.RuleModel;
import org.geomajas.sld.client.model.event.RuleSelectedEvent;
import org.geomajas.sld.client.model.event.RuleSelectedEvent.RuleSelectedHandler;
import org.geomajas.sld.client.presenter.event.InitSldLayoutEvent;
import org.geomajas.sld.client.presenter.event.InitSldLayoutEvent.InitSldLayoutHandler;
import org.geomajas.sld.client.presenter.event.SldContentChangedEvent.HasSldContentChangedHandlers;
import org.geomajas.sld.editor.client.GeometryType;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;

public class LineSymbolizerPresenter extends Presenter<LineSymbolizerPresenter.MyView, LineSymbolizerPresenter.MyProxy>
		implements RuleSelectedHandler, InitSldLayoutHandler {

	private Logger logger = Logger.getLogger(LineSymbolizerPresenter.class.getName());
	
	private LineSymbolizerInfo currentModel;

	/**
	 * {@link LineSymbolizerPresenter}'s proxy.
	 */
	@ProxyStandard
	public interface MyProxy extends Proxy<LineSymbolizerPresenter> {
	}

	/**
	 * {@link StyledLayerDescriptorPresenter}'s view.
	 */
	public interface MyView extends View, HasSldContentChangedHandlers {

		void modelToView(LineSymbolizerInfo lineSymbolizerInfo);

		void hide();

		void show();

		void clear();
	}

	/**
	 * Constructor.
	 * 
	 * @param eventBus
	 * @param view
	 * @param proxy
	 */
	@Inject
	public LineSymbolizerPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy) {
		super(eventBus, view, proxy);
	}

	@Override
	protected void onBind() {
		super.onBind();
		addRegisteredHandler(RuleSelectedEvent.getType(), this);
	}

	@Override
	protected void revealInParent() {
		RevealContentEvent.fire(this, RulePresenter.TYPE_SYMBOL_CONTENT, this);
	}

	@Override
	protected void onReveal() {
		super.onReveal();
	}

	/*
	 * (non-Javadoc) Refresh any information displayed by your presenter.
	 * 
	 * @see com.gwtplatform.mvp.client.PresenterWidget#onReset()
	 */
	@Override
	protected void onReset() {
		super.onReset();
	}

	public void onRuleSelected(RuleSelectedEvent event) {
		if (event.isClearAll()) {
			getView().clear();
			getView().hide();
		} else {
			RuleModel rule = event.getRuleModel();
			if (rule.getGeometryType().equals(GeometryType.LINE)) {
				forceReveal();
				currentModel = (LineSymbolizerInfo) rule.getSymbolizerTypeInfo(); 
				getView().modelToView(currentModel);
				getView().show();
			} else {
				getView().clear();
				getView().hide();
			}
		}
	}

	@ProxyEvent
	public void onInitSldLayout(InitSldLayoutEvent event) {
		forceReveal();
	}

}
