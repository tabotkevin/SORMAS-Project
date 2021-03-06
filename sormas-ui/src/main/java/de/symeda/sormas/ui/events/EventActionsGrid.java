/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.events;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.navigator.View;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.renderers.DateRenderer;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.event.EventActionIndexDto;
import de.symeda.sormas.api.event.EventCriteria;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.FilteredGrid;
import de.symeda.sormas.ui.utils.ShowDetailsListener;
import de.symeda.sormas.ui.utils.UuidRenderer;
import de.symeda.sormas.ui.utils.ViewConfiguration;

import java.util.Date;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class EventActionsGrid extends FilteredGrid<EventActionIndexDto, EventCriteria> {

	public static final String EVENT_DATE = Captions.singleDayEventDate;

	@SuppressWarnings("unchecked")
	public <V extends View> EventActionsGrid(EventCriteria eventCriteria, Class<V> viewClass) {

		super(EventActionIndexDto.class);
		setSizeFull();

		ViewConfiguration viewConfiguration = ViewModelProviders.of(viewClass).get(ViewConfiguration.class);
		setInEagerMode(viewConfiguration.isInEagerMode());

		setLazyDataProvider();
		setCriteria(eventCriteria);

		Language userLanguage = I18nProperties.getUserLanguage();

		setColumns(
			EventActionIndexDto.EVENT_UUID,
			EventActionIndexDto.EVENT_TITLE,
			createEventDateColumn(this, userLanguage),
			EventActionIndexDto.EVENT_STATUS,
			EventActionIndexDto.ACTION_TITLE,
			EventActionIndexDto.ACTION_CREATION_DATE,
			EventActionIndexDto.ACTION_CHANGE_DATE,
			EventActionIndexDto.ACTION_STATUS,
			EventActionIndexDto.ACTION_PRIORITY,
			EventActionIndexDto.ACTION_REPLYING_USER);

		((Column<EventActionIndexDto, String>) getColumn(EventActionIndexDto.EVENT_UUID)).setRenderer(new UuidRenderer());
		((Column<EventActionIndexDto, Date>) getColumn(EventActionIndexDto.ACTION_CREATION_DATE))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(userLanguage)));
		((Column<EventActionIndexDto, Date>) getColumn(EventActionIndexDto.ACTION_CHANGE_DATE))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(userLanguage)));

		for (Column<EventActionIndexDto, ?> column : getColumns()) {
			String columnId = column.getId();
			column.setCaption(I18nProperties.getPrefixCaption(EventActionIndexDto.I18N_PREFIX, columnId, column.getCaption()));
		}

		addItemClickListener(
			new ShowDetailsListener<>(EventActionIndexDto.EVENT_UUID, e -> ControllerProvider.getEventController().navigateToData(e.getEventUuid())));
	}

	private String createEventDateColumn(FilteredGrid<EventActionIndexDto, EventCriteria> grid, Language userLanguage) {
		Column<EventActionIndexDto, String> eventDateColumn = grid.addColumn(event -> {
			Date startDate = event.getEventStartDate();
			Date endDate = event.getEventEndDate();

			if (startDate == null) {
				return "";
			} else if (endDate == null) {
				return DateHelper.formatLocalDate(startDate, userLanguage);
			} else {
				return String
					.format("%s - %s", DateHelper.formatLocalDate(startDate, userLanguage), DateHelper.formatLocalDate(endDate, userLanguage));
			}
		});
		eventDateColumn.setId(EVENT_DATE);
		eventDateColumn.setSortProperty(EventActionIndexDto.EVENT_START_DATE);
		eventDateColumn.setSortable(true);

		return EVENT_DATE;
	}

	public void reload() {

		if (getSelectionModel().isUserSelectionAllowed()) {
			deselectAll();
		}

		getDataProvider().refreshAll();
	}

	public void setLazyDataProvider() {

		DataProvider<EventActionIndexDto, EventCriteria> dataProvider = DataProvider.fromFilteringCallbacks(
			query -> FacadeProvider.getActionFacade()
				.getEventActionList(
					query.getFilter().orElse(null),
					query.getOffset(),
					query.getLimit(),
					query.getSortOrders()
						.stream()
						.map(sortOrder -> new SortProperty(sortOrder.getSorted(), sortOrder.getDirection() == SortDirection.ASCENDING))
						.collect(Collectors.toList()))
				.stream(),
			query -> (int) FacadeProvider.getActionFacade().countEventAction(query.getFilter().orElse(null)));
		setDataProvider(dataProvider);
		setSelectionMode(SelectionMode.NONE);
	}
}
