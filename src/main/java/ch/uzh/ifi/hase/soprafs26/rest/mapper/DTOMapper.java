package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.PantryItem;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PantryItemGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "password", target = "password")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "bio", target = "bio")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "creationDate", target = "creationDate")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "barcode", target = "barcode")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "brand", target = "brand")
	@Mapping(source = "kcalPer100", target = "kcalPer100")
	@Mapping(source = "packageQuantity", target = "packageQuantity")
	@Mapping(source = "packageUnit", target = "packageUnit")
	@Mapping(source = "kcalPerPackage", target = "kcalPerPackage")
	@Mapping(source = "count", target = "count")
	@Mapping(source = "createdAt", target = "createdAt")
	@Mapping(source = "consumedAt", target = "consumedAt")
	@Mapping(source = "lastConsumedAt", target = "lastConsumedAt")
	PantryItemGetDTO convertEntityToPantryItemGetDTO(PantryItem pantryItem);
}
