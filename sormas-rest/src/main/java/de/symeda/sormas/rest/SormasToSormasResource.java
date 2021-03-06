/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.sormastosormas.SormasToSormasApiConstants;
import de.symeda.sormas.api.sormastosormas.SormasToSormasEncryptedDataDto;
import de.symeda.sormas.api.sormastosormas.SormasToSormasErrorResponse;
import de.symeda.sormas.api.sormastosormas.SormasToSormasException;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.ValidationRuntimeException;

@Path(SormasToSormasApiConstants.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@RolesAllowed(UserRole._SORMAS_TO_SORMAS_CLIENT)
public class SormasToSormasResource {

	@POST
	@Path(SormasToSormasApiConstants.CASE_ENDPOINT)
	public Response saveSharedCase(SormasToSormasEncryptedDataDto sharedCase) {
		try {
			FacadeProvider.getSormasToSormasFacade().saveSharedCase(sharedCase);
		} catch (ValidationRuntimeException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new SormasToSormasErrorResponse(e.getMessage())).build();
		} catch (SormasToSormasException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		return Response.noContent().build();
	}

	@POST
	@Path(SormasToSormasApiConstants.CONTACT_ENDPOINT)
	public Response saveSharedContact(SormasToSormasEncryptedDataDto sharedContact) {
		try {
			FacadeProvider.getSormasToSormasFacade().saveSharedContact(sharedContact);
		} catch (ValidationRuntimeException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new SormasToSormasErrorResponse(e.getMessage())).build();
		} catch (SormasToSormasException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

		return Response.noContent().build();
	}

}
